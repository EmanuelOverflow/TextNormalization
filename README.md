# TextNormalization
Normalize emphasized words

Phonetic without stemming
```java
TextNormalization tn = new TextNormalization();
String normalized = tn.normalize("heeeellloooo", false));
// ---> heeeellloooo = hello
```

Phonetic with stemming
```java
String normalized = tn.normalize("occcccccuring", true); 
// ---> occcccccuring = occur
```

Combination without stemming
```java
String [] words = TextNormalization.normalizeCombination("heeeellloooo", true);
// ---> hellloooo = helo, heloo, helloo, hello
```

Combination with stemming
```java
String [] words = TextNormalization.normalizeCombination("occcccccuring", true);
// ---> occcurrrring = ocur, occur, ocurr, occurr
```

See the doc for the full list of usages.
