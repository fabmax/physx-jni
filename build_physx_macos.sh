#!/bin/sh

cd PhysX4/physx
cmake --build ./compiler/jni_mac64 --config release

cp ./bin/jni.mac64/release/libPhysXJniBindings_64.dylib ../../physx-jni-natives-macos/src/main/resources/macos
