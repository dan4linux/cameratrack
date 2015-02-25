#!/bin/bash

cd `dirname $0`

export PATH=/usr/local/jsdk/bin:$PATH

java -Djava.library.path=rxtx -cp target/cameratest-0.0.1-uber.jar:rxtx/RXTXcomm.jar net.swansonstuff.cameratest.CameraTrack

