# TypeScript compiler for AEM Client Libraries
[![Build Status](https://travis-ci.org/bpauli/aem-typescript.svg?branch=master)](https://travis-ci.org/bpauli/aem-typescript)

# Installation
Install the bundle with maven or via the Felix console (/system/console).
```
	$ mvn clean install -PautoInstallBundle -Daem.host=localhost -Daem.port=4504
```

If the bundle doesn't get in running state because the dependency to jdk.nashorn.api.scripting can't be resolved, you have to add
``` 
    ,jdk.nashorn.api.scripting;version\="0.0.0.1_008_JavaSE"
```     
to the jre-1.8 property in your sling.properties file (crx-quickstart/conf) 
    
## Usage
Every file of your typical client library with a ***.ts*** extension will be processed by this compiler.

Client Library example: 
```
├── myClientlib
│   ├── .content.xml
│   ├── ts
│   │   ├── Application.ts
│   ├── js.txt
```

.content.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="cq:ClientLibraryFolder"
    categories="[my-client-lib]"/>
```

js.txt
```
ts/Application.ts
```

## System requirements

- AEM 6.2
- JVM 1.8u65 or newer (https://bugs.openjdk.java.net/browse/JDK-8074545)

## Credits
Based on [jtsc](https://github.com/wmono/jtsc)
