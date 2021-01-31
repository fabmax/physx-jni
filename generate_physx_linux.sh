#!/bin/sh

cd ./PhysX/physx
rm -rf compiler/jni_linux-*
./generate_projects.sh jni_linux
