#!/usr/bin/env bash
set -euo pipefail

# Usage:
#  ./deploy.sh              # build image from prebuilt JAR and run container
#  ./deploy.sh build        # only build Docker image
#  ./deploy.sh stop         # stop and remove container
#  ./deploy.sh logs         # tail container logs
#  PORT=10088 ./deploy.sh   # override port
#  IMAGE_TAG=mytag ./deploy.sh  # override image tag
#  JAVA_OPTS="-Xms256m -Xmx512m" ./deploy.sh

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
IMAGE_NAME="location-server"
IMAGE_TAG="${IMAGE_TAG:-latest}"
CONTAINER_NAME="location-server"
PORT="${PORT:-10088}"

cd "$APP_DIR"

# Build jar externally using Maven (outside Docker)
if [[ "${1:-}" != "stop" && "${1:-}" != "logs" ]]; then
  echo "Building JAR with Maven (external)..."
  mvn -q -DskipTests -f "$APP_DIR/pom.xml" package
fi

case "${1:-}" in
  stop)
    docker rm -f "$CONTAINER_NAME" 2>/dev/null || true
    echo "Container '$CONTAINER_NAME' stopped and removed."
    ;;
  logs)
    docker logs -f "$CONTAINER_NAME"
    ;;
  build)
    echo "Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
    docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .
    ;;
  *)
    echo "Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
    docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

    echo "Stopping existing container (if any)..."
    docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

    echo "Running container on port ${PORT}..."
    docker run -d \
      --name "$CONTAINER_NAME" \
      -p "${PORT}:${PORT}" \
      -e PORT="$PORT" \
      -e JAVA_OPTS="${JAVA_OPTS:-}" \
      "${IMAGE_NAME}:${IMAGE_TAG}"

    echo "Container started. Use 'docker logs -f ${CONTAINER_NAME}' to view logs."
    ;;
}