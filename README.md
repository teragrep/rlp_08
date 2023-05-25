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

Defaults to port 1601 with no tls.

Listening port can be changed with `-Dport=1234`.

TLS can be enabled with `-Dtls=true`