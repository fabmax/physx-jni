#!/bin/sh

cd PhysX/physx/compiler/jni_linux-release/
make -j8

cp ../../bin/jni.linux/release/libPhysXJniBindings_64.so ../../../../physx-jni-native-linux64/src/main/resources/linux64

cp ../../bin/jni.linux/release/libPhysXJniBindings_64.so ../../../../physx-jni-native-linux64cuda/src/main/resources/linux64
cp ../../bin/linux.clang/release/libPhysXGpu_64.so ../../../../physx-jni-native-linux64cuda/src/main/resources/linux64
