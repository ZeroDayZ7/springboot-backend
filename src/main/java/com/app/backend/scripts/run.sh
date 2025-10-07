#!/bin/bash

echo "=== Starting backend ==="
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootRun

