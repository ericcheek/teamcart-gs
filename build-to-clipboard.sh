#!/bin/sh

lein cljsbuild once main ; cat export/Code.gs | pbcopy


