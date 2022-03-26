#!/bin/sh

cd PhysX/physx/compiler/jni_linux-release/
make -j8

cp ../../bin/jni.linux/release/libPhysXJniBindings_64.so ../../../../physx-jni-natives-linux/src/main/resources/linux

cp ../../bin/jni.linux/release/libPhysXJniBindings_64.so ../../../../physx-jni-natives-linux-cuda/src/main/resources/linux
cp ../../bin/linux.clang/release/libPhysXGpu_64.so ../../../../physx-jni-natives-linux-cuda/src/main/resources/linux
