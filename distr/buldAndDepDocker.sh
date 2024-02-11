#!/bin/bash
cd ..
mv target/bot-1.0.jar distr/bot.jar
docker build -t bot distr
docker login
docker tag bot bennyhils/bot:outline_ch_24.02.11
docker push bennyhils/bot:outline_ch_24.02.11
