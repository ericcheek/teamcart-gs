#!/bin/sh
echo export/Code.gs | entr sh -c 'cat export/Code.gs | pbcopy' &
ENTRPID=$!
lein cljsbuild auto main
kill -9 $ENTRPID


