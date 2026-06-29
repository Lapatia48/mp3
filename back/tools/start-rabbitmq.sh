#!/usr/bin/env bash
# Demarre RabbitMQ (avec console de gestion) dans Docker.
#
# Contournement specifique a Docker Desktop / Windows : l'image officielle
# echoue avec "Error when reading /var/lib/rabbitmq/.erlang.cookie: eacces".
# On pre-cree le cookie (proprietaire rabbitmq, mode 600) puis on lance via gosu.
#
# Console : http://localhost:15672  (guest / guest)
# AMQP    : localhost:5672
set -e

docker rm -f rabbitmq >/dev/null 2>&1 || true
docker run -d --name rabbitmq --hostname rabbit \
  -p 5672:5672 -p 15672:15672 \
  --entrypoint sh rabbitmq:3-management -c '
    chmod 700 /var/lib/rabbitmq
    printf "mp3cookie123" > /var/lib/rabbitmq/.erlang.cookie
    chown -R rabbitmq:rabbitmq /var/lib/rabbitmq
    chmod 600 /var/lib/rabbitmq/.erlang.cookie
    exec gosu rabbitmq rabbitmq-server'

echo "RabbitMQ demarre. Attente de disponibilite..."
for i in $(seq 1 30); do
  if docker exec rabbitmq rabbitmq-diagnostics -q ping >/dev/null 2>&1; then
    echo "OK - broker pret (console : http://localhost:15672, guest/guest)"
    exit 0
  fi
  sleep 2
done
echo "Le broker n'a pas repondu a temps ; verifier 'docker logs rabbitmq'." >&2
exit 1
