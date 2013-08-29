#!/bin/sh


./CheckDic auteurs.dic --delaf -a common/Alphabet.txt
./Compress auteurs.dic
./CheckDic stopwords.dic --delaf -a common/Alphabet.txt
./Compress stopwords.dic
