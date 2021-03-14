#!/bin/bash
set -x
for i in {1..10}; do
  virsh net-update --current default add-last ip-dhcp-host "<host mac=\"00:1E:62:AA:AA:$(printf "%02x" $i)\" name=\"n${i}\" ip=\"192.168.122.1$(printf "%02d" $i)\"/>"
done

virsh net-autostart default;
virsh net-start default