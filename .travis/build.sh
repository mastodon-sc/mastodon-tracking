#!/bin/sh
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh $encrypted_99cb8222ddc6_key $encrypted_99cb8222ddc6_iv
