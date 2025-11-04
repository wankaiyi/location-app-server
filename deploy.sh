#!/bin/bash

BRANCH=${1:-master}

git checkout "$BRANCH"
git pull origin "$BRANCH"

mvn clean package -DskipTests

IMAGE_NAME="location-app-server"
FULL_IMAGE_NAME="${IMAGE_NAME}"

if docker ps -q --filter "name=location-app-server" | grep -q .; then
    echo "发现正在运行的容器 location-app-server，正在删除..."
    docker rm -f location-app-server
fi

if docker image inspect "$FULL_IMAGE_NAME" &> /dev/null; then
    echo "发现已存在的镜像 $FULL_IMAGE_NAME，正在删除..."
    docker rmi -f "$FULL_IMAGE_NAME"
fi

docker build -t "$FULL_IMAGE_NAME" .

docker run -d --name location-app-server -p 10088:10088 \
    "$FULL_IMAGE_NAME"
