#!/bin/bash
# Reference: https://www.tecmint.com/install-create-run-lxc-linux-containers-on-centos/
yum install epel-release
yum install debootstrap perl libvirt
yum install lxc lxc-templates
systemctl start lxc.service
systemctl start libvirtd