#!/bin/sh

cd ./PhysX/physx
rm -rf compiler/jni-linux-*
./generate_projects.sh jni-linux
