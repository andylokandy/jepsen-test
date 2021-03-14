#!/bin/bash
yum install -y java ncurses-devel gcc-c++ gnuplot bind-utils

# install leiningen
mkdir -p ~/bin
cd ~/bin
curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod a+x lein

# build start-stop-daemon in nodes
cd ~
wget "https://mirrors.tuna.tsinghua.edu.cn/debian/pool/main/d/dpkg/dpkg_1.17.27.tar.xz"
tar -xf dpkg_1.17.27.tar.xz
cd dpkg-1.17.27/
./configure
make