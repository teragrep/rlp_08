# Null routing relp server

```
 _____ _     _                _ _ _   _                        _ _ 
|_   _| |__ (_)___  __      _(_) | | | | ___  ___  ___    __ _| | |
  | | | '_ \| / __| \ \ /\ / / | | | | |/ _ \/ __|/ _ \  / _` | | |
  | | | | | | \__ \  \ V  V /| | | | | | (_) \__ \  __/ | (_| | | |
  |_| |_| |_|_|___/   \_/\_/ |_|_|_| |_|\___/|___/\___|  \__,_|_|_|


 _   _  ___  _   _ _ __   _ __ ___  ___ ___  _ __ __| |___ 
| | | |/ _ \| | | | '__| | '__/ _ \/ __/ _ \| '__/ _` / __|
| |_| | (_) | |_| | |    | | |  __/ (_| (_) | | | (_| \__ \
 \__, |\___/ \__,_|_|    |_|  \___|\___\___/|_|  \__,_|___/
 |___/                                                     
```

---

Build with
```sh
mvn clean package
```

Run with
```sh
java -jar rlp_08.jar
```

Listening port can be changed with `-Dport=1234`. Default `1234`

TLS can be enabled with `-Dtls=true`. Default `false`

Server thread count can be set with `-Dthreads=8`. Default 1.

Events received reporting interval can be enabled with `-DmetricsInterval=60`. Default 0 (disabled)

## Contributing
  
You can involve yourself with our project by [opening an issue](https://github.com/teragrep/rlp_08/issues/new/choose) or submitting a pull request.
 
Contribution requirements:
 
1. **All changes must be accompanied by a new or changed test.** If you think testing is not required in your pull request, include a sufficient explanation as why you think so.
2. Security checks must pass
3. Pull requests must align with the principles and [values](http://www.extremeprogramming.org/values.html) of extreme programming.
4. Pull requests must follow the principles of Object Thinking and Elegant Objects (EO).
 
Read more in our [Contributing Guideline](https://github.com/teragrep/teragrep/blob/main/contributing.adoc).
 
### Contributor License Agreement
 
Contributors must sign [Teragrep Contributor License Agreement](https://github.com/teragrep/teragrep/blob/main/cla.adoc) before a pull request is accepted to organization's repositories.
 
You need to submit the CLA only once. After submitting the CLA you can contribute to all Teragrep's repositories. 
