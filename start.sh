docker build -t location-server:latest .

docker run -d --name location-server -p 10088:10088 location-server
