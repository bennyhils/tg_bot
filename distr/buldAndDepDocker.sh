#!/bin/bash
cd ..
mv target/bot-1.0.jar distr/bot.jar
docker build -t bot distr
docker login
docker tag bot bennyhils/bot:ch_24.03.17
docker push bennyhils/bot:ch_24.03.17
