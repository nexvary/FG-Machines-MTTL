package main

import (
	"bufio"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"
)

const (
	getInfo = "up:getinfo:all"
)

var (
	bootRe  = regexp.MustCompile(`^up:bootinfo:([^;\r\n]{1,32});([0-9A-Fa-f]{12});([0-9A-Fa-f]{12});([^;\r\n]{1,64});connect$`)
	onoffRe = regexp.MustCompile(`^up:(?:event:)?onoff:([1-4]):(on|off)$`)
)

type device struct {
	mac       string
	model     string
	firmware  string
	remote    string
	lastSeen  int64
	conn      net.Conn
	writeMu   sync.Mutex
	closeOnce sync.Once
}

type gateway struct {
	mu      sync.RWMutex
	devices map[string]*device
}

func newGateway() *gateway {
	return &gateway{devices: map[string]*device{}}
}

func normalizeMAC(v string) string {
	v = strings.ToUpper(strings.TrimSpace(v))
	v = strings.ReplaceAll(v, ":", "")
	v = strings.ReplaceAll(v, "-", "")
	return v
}

func parseBoot(frame string) (model, mac, firmware string, ok bool) {
	m := bootRe.FindStringSubmatch(strings.TrimSpace(frame))
	if m == nil || !strings.EqualFold(m[2], m[3]) || !strings.EqualFold(m[1], "lgutap") {
		return "", "", "", false
	}
	return m[1], strings.ToUpper(m[2]), m[4], true
}

func outletCommand(outlet int, on bool) string {
	state := "off"
	if on {
		state = "on"
	}
	return fmt.Sprintf("up:onoff:%d:%s", outlet, state)
}

func (g *gateway) add(d *device) {
	g.mu.Lock()
	defer g.mu.Unlock()
	if old := g.devices[d.mac]; old != nil && old != d {
		old.close()
	}
	g.devices[d.mac] = d
}

func (g *gateway) remove(d *device) {
	g.mu.Lock()
	defer g.mu.Unlock()
	if g.devices[d.mac] == d {
		delete(g.devices, d.mac)
	}
}

func (d *device) close() {
	d.closeOnce.Do(func() { _ = d.conn.Close() })
}

func (d *device) send(frame string) error {
	d.writeMu.Lock()
	defer d.writeMu.Unlock()
	_ = d.conn.SetWriteDeadline(time.Now().Add(3 * time.Second))
	_, err := fmt.Fprintf(d.conn, "%s\r\n", frame)
	return err
}

func (g *gateway) serveController(bind string) error {
	ln, err := net.Listen("tcp", bind)
	if err != nil {
		return err
	}
	log.Printf("MTTL controller listening on %s", bind)
	for {
		conn, err := ln.Accept()
		if err != nil {
			return err
		}
		go g.handleDevice(conn)
	}
}

func (g *gateway) handleDevice(conn net.Conn) {
	defer conn.Close()
	_ = conn.SetReadDeadline(time.Time{})
	scanner := bufio.NewScanner(conn)
	scanner.Buffer(make([]byte, 4096), 128*1024)
	var current *device

	for scanner.Scan() {
		frame := strings.TrimSpace(scanner.Text())
		if frame == "" {
			continue
		}
		if current == nil {
			model, mac, fw, ok := parseBoot(frame)
			if !ok {
				continue
			}
			current = &device{
				mac:      mac,
				model:    model,
				firmware: fw,
				remote:   conn.RemoteAddr().String(),
				lastSeen: time.Now().Unix(),
				conn:     conn,
			}
			g.add(current)
			log.Printf("MTTL device connected: %s (%s)", mac, fw)
			_ = current.send(getInfo)
			continue
		}
		current.lastSeen = time.Now().Unix()
		if m := onoffRe.FindStringSubmatch(frame); m != nil {
			log.Printf("MTTL state event: %s outlet=%s state=%s", current.mac, m[1], m[2])
		}
	}
	if current != nil {
		g.remove(current)
		log.Printf("MTTL device disconnected: %s", current.mac)
	}
}

func (g *gateway) pollLoop() {
	t := time.NewTicker(10 * time.Second)
	defer t.Stop()
	for range t.C {
		g.mu.RLock()
		list := make([]*device, 0, len(g.devices))
		for _, d := range g.devices {
			list = append(list, d)
		}
		g.mu.RUnlock()
		for _, d := range list {
			if err := d.send(getInfo); err != nil {
				d.close()
			}
		}
	}
}

func requireToken(next http.Handler, token string) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		got := strings.TrimSpace(strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer "))
		if got == "" || got != token {
			writeJSON(w, http.StatusUnauthorized, map[string]any{"error": "unauthorized"})
			return
		}
		next.ServeHTTP(w, r)
	})
}

func writeJSON(w http.ResponseWriter, code int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	_ = json.NewEncoder(w).Encode(v)
}

func (g *gateway) apiHandler() http.Handler {
	mux := http.NewServeMux()

	mux.HandleFunc("/api/v1/health", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"error": "method_not_allowed"})
			return
		}
		g.mu.RLock()
		count := len(g.devices)
		g.mu.RUnlock()
		writeJSON(w, http.StatusOK, map[string]any{
			"ok": true, "service": "fg-link-gateway", "connected_devices": count,
			"time": time.Now().Unix(),
		})
	})

	mux.HandleFunc("/api/v1/devices", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"error": "method_not_allowed"})
			return
		}
		g.mu.RLock()
		out := make([]map[string]any, 0, len(g.devices))
		for _, d := range g.devices {
			out = append(out, map[string]any{
				"mac": d.mac, "name": "MTTL-W01 · " + d.mac, "room": "",
				"connected": true, "last_seen": d.lastSeen, "firmware": d.firmware,
			})
		}
		g.mu.RUnlock()
		writeJSON(w, http.StatusOK, map[string]any{"devices": out})
	})

	mux.HandleFunc("/api/v1/history/", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"error": "method_not_allowed"})
			return
		}
		mac := normalizeMAC(strings.TrimPrefix(r.URL.Path, "/api/v1/history/"))
		writeJSON(w, http.StatusOK, map[string]any{"mac": mac, "samples": []any{}})
	})

	mux.HandleFunc("/api/v1/devices/", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, http.StatusMethodNotAllowed, map[string]any{"error": "method_not_allowed"})
			return
		}
		parts := strings.Split(strings.Trim(strings.TrimPrefix(r.URL.Path, "/api/v1/devices/"), "/"), "/")
		if len(parts) != 3 || parts[1] != "outlets" {
			writeJSON(w, http.StatusNotFound, map[string]any{"error": "not_found"})
			return
		}
		mac := normalizeMAC(parts[0])
		outlet, err := strconv.Atoi(parts[2])
		if err != nil || outlet < 1 || outlet > 4 {
			writeJSON(w, http.StatusBadRequest, map[string]any{"error": "invalid_outlet"})
			return
		}
		state := strings.ToLower(strings.TrimSpace(r.URL.Query().Get("state")))
		if state != "on" && state != "off" {
			writeJSON(w, http.StatusBadRequest, map[string]any{"error": "invalid_state"})
			return
		}
		g.mu.RLock()
		d := g.devices[mac]
		g.mu.RUnlock()
		if d == nil {
			writeJSON(w, http.StatusNotFound, map[string]any{"error": "device_offline"})
			return
		}
		if err := d.send(outletCommand(outlet, state == "on")); err != nil {
			writeJSON(w, http.StatusServiceUnavailable, map[string]any{"error": "write_failed"})
			return
		}
		writeJSON(w, http.StatusOK, map[string]any{
			"ok": true, "mac": mac, "outlet": outlet, "state": state,
		})
	})

	return mux
}

func main() {
	var apiBind string
	var controllerBind string
	flag.StringVar(&apiBind, "api-bind", "127.0.0.1:18086", "HTTP API bind address; set to the ZeroTier IP after enrollment")
	flag.StringVar(&controllerBind, "controller-bind", "0.0.0.0:10086", "MTTL controller bind address")
	flag.Parse()

	token := strings.TrimSpace(os.Getenv("FG_GATEWAY_TOKEN"))
	if len(token) < 24 {
		log.Fatal("FG_GATEWAY_TOKEN must be set to a random value of at least 24 characters")
	}

	g := newGateway()
	go g.pollLoop()
	go func() {
		if err := g.serveController(controllerBind); err != nil {
			log.Fatalf("MTTL controller failed: %v", err)
		}
	}()

	srv := &http.Server{
		Addr:              apiBind,
		Handler:           requireToken(g.apiHandler(), token),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      10 * time.Second,
		IdleTimeout:       30 * time.Second,
	}
	log.Printf("FG Link Gateway API listening on %s", apiBind)
	log.Fatal(srv.ListenAndServe())
}
