#!/bin/bash

# Kill :1099
kill -9 $(lsof -t -i:1099) && kill -9 $(lsof -t -i:1100)