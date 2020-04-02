#!/bin/sh
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh $encrypted_a782dab4a3fb_key $encrypted_a782dab4a3fb_iv
