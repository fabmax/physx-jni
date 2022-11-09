#!/bin/sh

cd ./PhysX4/physx
rm -rf compiler/jni_linux-*
./generate_projects.sh jni_linux
