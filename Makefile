build/%.class: src/%.java
	@mkdir -p build
	@javac -d build -sourcepath src src/$(@:build/%.class=%.java)
	@echo "src/$(@:build/%.class=%.java) -> ${@}"

build: \
	build/Key.class \
	build/Peer.class \
	build/PeerImpl.class \
	build/console/Arguments.class \
	build/console/Parser.class \
	build/console/Terminal.class \
	build/logging/Loggable.class \
	build/networking/Address.class \
	build/networking/Channel.class \
	build/networking/Host.class \
	build/networking/Port.class \
	build/networking/Servable.class \
	build/remote/Proxy.class \
	build/threading/Schedule.class \
	build/threading/Task.class \
	build/threading/Worker.class

Peer: build
	@java -cp build PeerImpl ${host} ${join}

.PHONY: Peer
