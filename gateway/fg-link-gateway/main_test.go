package main

import "testing"

func TestParseBoot(t *testing.T) {
	model, mac, fw, ok := parseBoot("up:bootinfo:lgutap;88D039AABBCC;88D039AABBCC;1.0.110;connect")
	if !ok || model != "lgutap" || mac != "88D039AABBCC" || fw != "1.0.110" {
		t.Fatalf("unexpected parse result: %v %q %q %q", ok, model, mac, fw)
	}
}

func TestRejectMismatchedClientID(t *testing.T) {
	_, _, _, ok := parseBoot("up:bootinfo:lgutap;88D039AABBCC;88D039AABBDD;1.0.110;connect")
	if ok {
		t.Fatal("mismatched client id must be rejected")
	}
}

func TestOutletCommand(t *testing.T) {
	if got := outletCommand(3, true); got != "up:onoff:3:on" {
		t.Fatalf("unexpected command: %s", got)
	}
}
