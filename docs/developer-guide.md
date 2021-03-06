# ð Developer Guide

---------------------------

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [ð æ¡æ¶/ä¸­é´ä»¶éæ`TTL`ä¼ é](#-%E6%A1%86%E6%9E%B6%E4%B8%AD%E9%97%B4%E4%BB%B6%E9%9B%86%E6%88%90ttl%E4%BC%A0%E9%80%92)
- [ð å³äº`Java Agent`](#-%E5%85%B3%E4%BA%8Ejava-agent)
    - [`Java Agent`æ¹å¼å¯¹åºç¨ä»£ç æ ä¾µå¥](#java-agent%E6%96%B9%E5%BC%8F%E5%AF%B9%E5%BA%94%E7%94%A8%E4%BB%A3%E7%A0%81%E6%97%A0%E4%BE%B5%E5%85%A5)
    - [å·²æ`Java Agent`ä¸­åµå¥`TTL Agent`](#%E5%B7%B2%E6%9C%89java-agent%E4%B8%AD%E5%B5%8C%E5%85%A5ttl-agent)
- [ð¢ Bootstrapä¸æ·»å éç¨åºç`Jar`çé®é¢åè§£å³æ¹æ³](#-bootstrap%E4%B8%8A%E6%B7%BB%E5%8A%A0%E9%80%9A%E7%94%A8%E5%BA%93%E7%9A%84jar%E7%9A%84%E9%97%AE%E9%A2%98%E5%8F%8A%E8%A7%A3%E5%86%B3%E6%96%B9%E6%B3%95)
- [ð ç¸å³èµæ](#-%E7%9B%B8%E5%85%B3%E8%B5%84%E6%96%99)
    - [Jdk core classes](#jdk-core-classes)
    - [Java Agent](#java-agent)
    - [Javassist](#javassist)
    - [Shadeæä»¶](#shade%E6%8F%92%E4%BB%B6)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

---------------------------

# ð æ¡æ¶/ä¸­é´ä»¶éæ`TTL`ä¼ é

æ¡æ¶/ä¸­é´ä»¶éæ`TTL`ä¼ éï¼éè¿[`TransmittableThreadLocal.Transmitter`](../src/main/java/com/alibaba/ttl/TransmittableThreadLocal.java#L240)
æåå½åçº¿ç¨çææ`TTL`å¼å¹¶å¨å¶ä»çº¿ç¨è¿è¡åæ¾ï¼å¨åæ¾çº¿ç¨æ§è¡å®ä¸å¡æä½åï¼æ¢å¤ä¸ºåæ¾çº¿ç¨åæ¥ç`TTL`å¼ã

[`TransmittableThreadLocal.Transmitter`](../src/main/java/com/alibaba/ttl/TransmittableThreadLocal.java#L201)æä¾äºææ`TTL`å¼çæåãåæ¾åæ¢å¤æ¹æ³ï¼å³`CRR`æä½ï¼ï¼

1. `capture`æ¹æ³ï¼æåçº¿ç¨ï¼çº¿ç¨Aï¼çææ`TTL`å¼ã
2. `replay`æ¹æ³ï¼å¨å¦ä¸ä¸ªçº¿ç¨ï¼çº¿ç¨Bï¼ä¸­ï¼åæ¾å¨`capture`æ¹æ³ä¸­æåç`TTL`å¼ï¼å¹¶è¿å åæ¾å`TTL`å¼çå¤ä»½
3. `restore`æ¹æ³ï¼æ¢å¤çº¿ç¨Bæ§è¡`replay`æ¹æ³ä¹åç`TTL`å¼ï¼å³å¤ä»½ï¼

ç¤ºä¾ä»£ç ï¼

```java
// ===========================================================================
// çº¿ç¨ A
// ===========================================================================

TransmittableThreadLocal<String> parent = new TransmittableThreadLocal<String>();
parent.set("value-set-in-parent");

// (1) æåå½åçº¿ç¨çææTTLå¼
final Object captured = TransmittableThreadLocal.Transmitter.capture();

// ===========================================================================
// çº¿ç¨ Bï¼å¼æ­¥çº¿ç¨ï¼
// ===========================================================================

// (2) å¨çº¿ç¨ Bä¸­åæ¾å¨captureæ¹æ³ä¸­æåçTTLå¼ï¼å¹¶è¿å åæ¾åTTLå¼çå¤ä»½
final Object backup = TransmittableThreadLocal.Transmitter.replay(captured);
try {
    // ä½ çä¸å¡é»è¾ï¼è¿éä½ å¯ä»¥è·åå°å¤é¢è®¾ç½®çTTLå¼
    String value = parent.get();

    System.out.println("Hello: " + value);
    ...
    String result = "World: " + value;
} finally {
    // (3) æ¢å¤çº¿ç¨ Bæ§è¡replayæ¹æ³ä¹åçTTLå¼ï¼å³å¤ä»½ï¼
    TransmittableThreadLocal.Transmitter.restore(backup);
}
```

`TTL`ä¼ éçå·ä½å®ç°ç¤ºä¾åè§ [`TtlRunnable.java`](../src/main/java/com/alibaba/ttl/TtlRunnable.java)ã[`TtlCallable.java`](../src/main/java/com/alibaba/ttl/TtlCallable.java)ã

å½ç¶å¯ä»¥ä½¿ç¨`TransmittableThreadLocal.Transmitter`çå·¥å·æ¹æ³`runSupplierWithCaptured`å`runCallableWithCaptured`åå¯ç±ç`Java 8 Lambda`è¯­æ³
æ¥ç®å`replay`å`restore`æä½ï¼ç¤ºä¾ä»£ç ï¼

```java
// ===========================================================================
// çº¿ç¨ A
// ===========================================================================

TransmittableThreadLocal<String> parent = new TransmittableThreadLocal<String>();
parent.set("value-set-in-parent");

// (1) æåå½åçº¿ç¨çææTTLå¼
final Object captured = TransmittableThreadLocal.Transmitter.capture();

// ===========================================================================
// çº¿ç¨ Bï¼å¼æ­¥çº¿ç¨ï¼
// ===========================================================================

String result = runSupplierWithCaptured(captured, () -> {
    // ä½ çä¸å¡é»è¾ï¼è¿éä½ å¯ä»¥è·åå°å¤é¢è®¾ç½®çTTLå¼
    String value = parent.get();
    System.out.println("Hello: " + value);
    ...
    return "World: " + value;
}); // (2) + (3)
```

æ´å¤`TTL`ä¼ éçè¯´æè¯¦è§[`TransmittableThreadLocal.Transmitter`](../src/main/java/com/alibaba/ttl/TransmittableThreadLocal.java#L240)ç`JavaDoc`ã

# ð å³äº`Java Agent`

## `Java Agent`æ¹å¼å¯¹åºç¨ä»£ç æ ä¾µå¥

[User Guide - 2.3 ä½¿ç¨`Java Agent`æ¥ä¿®é¥°`JDK`çº¿ç¨æ± å®ç°ç±»](../README.md#23-%E4%BD%BF%E7%94%A8java-agent%E6%9D%A5%E4%BF%AE%E9%A5%B0jdk%E7%BA%BF%E7%A8%8B%E6%B1%A0%E5%AE%9E%E7%8E%B0%E7%B1%BB) è¯´å°äºï¼ç¸å¯¹ä¿®é¥°`Runnable`ææ¯çº¿ç¨æ± çæ¹å¼ï¼`Java Agent`æ¹å¼æ¯å¯¹åºç¨ä»£ç æ ä¾µå¥çãä¸é¢åä¸äºå±å¼è¯´æã

<img src="scenario-framework-sdk-arch.png" alt="ææ¶å¾" width="260" />

ææ¡æ¶å¾ï¼æåé¢ç¤ºä¾ä»£ç æä½å¯ä»¥åæä¸é¢å é¨åï¼

1. è¯»åä¿¡æ¯è®¾ç½®å°`TTL`ã  
    è¿é¨åå¨å®¹å¨ä¸­å®æï¼æ éåºç¨åä¸ã
2. æäº¤`Runnable`å°çº¿ç¨æ± ãè¦æä¿®é¥°æä½`Runnable`ï¼æ è®ºæ¯ç´æ¥ä¿®é¥°`Runnable`è¿æ¯ä¿®é¥°çº¿ç¨æ± ï¼ã  
    è¿é¨åæä½ä¸å®æ¯å¨ç¨æ·åºç¨ä¸­è§¦åã
3. è¯»å`TTL`ï¼åä¸å¡æ£æ¥ã  
    å¨`SDK`ä¸­å®æï¼æ éåºç¨åä¸ã

åªæç¬¬2é¨åçæä½ååºç¨ä»£ç ç¸å³ã

å¦æä¸éè¿`Java Agent`ä¿®é¥°çº¿ç¨æ± ï¼åä¿®é¥°æä½éè¦åºç¨ä»£ç æ¥å®æã

ä½¿ç¨`Java Agent`æ¹å¼ï¼åºç¨æ éä¿®æ¹ä»£ç ï¼å³åå° ç¸å¯¹åºç¨ä»£ç  éæå°å®æè·¨çº¿ç¨æ± çä¸ä¸æä¼ éã

æ´å¤å³äºåºç¨åºæ¯çäºè§£è¯´æåè§ææ¡£[éæ±åºæ¯](requirement-scenario.md)ã

## å·²æ`Java Agent`ä¸­åµå¥`TTL Agent`

è¿æ ·å¯ä»¥åå°`Java`å¯å¨å½ä»¤è¡ä¸ç`Agent`çéç½®ã

å¨èªå·±ç`Agent`ä¸­å ä¸`TTL Agent`çé»è¾ï¼ç¤ºä¾ä»£ç å¦ä¸ï¼[`YourXxxAgent.java`](../src/test/java/com/alibaba/demo/ttl/agent/YourXxxAgent.java)ï¼ï¼

```java
import com.alibaba.ttl.threadpool.agent.TtlAgent;
import com.alibaba.ttl.threadpool.agent.TtlTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public final class YourXxxAgent {
    private static final Logger logger = Logger.getLogger(YourXxxAgent.class.getName());

    public static void premain(String agentArgs, Instrumentation inst) {
        TtlAgent.premain(agentArgs, inst); // add TTL Transformer

        // add your Transformer
        ...
    }
}
```

å³äº`Java Agent`å`ClassFileTransformer`çå¦ä½å®ç°å¯ä»¥åèï¼[`TtlAgent.java`](../src/main/java/com/alibaba/ttl/threadpool/agent/TtlAgent.java)ã[`TtlTransformer.java`](../src/main/java/com/alibaba/ttl/threadpool/agent/TtlTransformer.java)ã

æ³¨æå¨`bootclasspath`ä¸ï¼è¿æ¯è¦å ä¸`TTL`ä¾èµçJarï¼

```bash
-Xbootclasspath/a:/path/to/transmittable-thread-local-2.0.0.jar:/path/to/your/agent/jar/files
```

# ð¢ Bootstrapä¸æ·»å éç¨åºç`Jar`çé®é¢åè§£å³æ¹æ³

éè¿`Java`å½ä»¤åæ°`-Xbootclasspath`æåºç`Jar`å `Bootstrap` `ClassPath`ä¸ã`Bootstrap` `ClassPath`ä¸ç`Jar`ä¸­ç±»ä¼ä¼åäºåºç¨`ClassPath`ç`Jar`è¢«å è½½ï¼å¹¶ä¸ä¸è½è¢«è¦çã

`TTL`å¨`Bootstrap` `ClassPath`ä¸æ·»å äº`Javassist`çä¾èµï¼å¦æåºç¨ä¸­å¦æä½¿ç¨äº`Javassist`ï¼å®éä¸ä¼ä¼åä½¿ç¨`Bootstrap` `ClassPath`ä¸ç`Javassist`ï¼å³åºç¨ä¸è½éæ©`Javassist`ççæ¬ï¼åºç¨éè¦ç`Javassist`å`TTL`ç`Javassist`æå¼å®¹æ§çé£é©ã

å¯ä»¥éè¿`repackage`ï¼éæ°å½åååï¼æ¥è§£å³è¿ä¸ªé®é¢ã

`Maven`æä¾äº[`Shade`æä»¶](http://maven.apache.org/plugins/maven-shade-plugin/)ï¼å¯ä»¥å®æ`repackage`æä½ï¼å¹¶æ`Javassist`çç±»å å°`TTL`ç`Jar`ä¸­ã

è¿æ ·å°±ä¸éè¦ä¾èµå¤é¨ç`Javassist`ä¾èµï¼ä¹è§é¿äºä¾èµå²çªçé®é¢ã

# ð ç¸å³èµæ

## Jdk core classes

- [WeakHashMap](https://docs.oracle.com/javase/10/docs/api/java/util/WeakHashMap.html)
- [InheritableThreadLocal](https://docs.oracle.com/javase/10/docs/api/java/lang/InheritableThreadLocal.html)

## Java Agent

- å®æ¹ææ¡£
    - [`Java Agent`è§è - `JavaDoc`](https://docs.oracle.com/javase/10/docs/api/java/lang/instrument/package-summary.html#package.description)
    - [JAR File Specification - JAR Manifest](https://docs.oracle.com/javase/10/docs/specs/jar/jar.html#jar-manifest)
    - [Working with Manifest Files - The Javaâ¢ TutorialsHide](https://docs.oracle.com/javase/tutorial/deployment/jar/manifestindex.html)
- [Java SE 6 æ°ç¹æ§: Instrumentation æ°åè½](http://www.ibm.com/developerworks/cn/java/j-lo-jse61/)
- [Creation, dynamic loading and instrumentation with javaagents](http://dhruba.name/2010/02/07/creation-dynamic-loading-and-instrumentation-with-javaagents/)
- [JavaAgentå è½½æºå¶åæ](http://alipaymiddleware.com/jvm/javaagent%E5%8A%A0%E8%BD%BD%E6%9C%BA%E5%88%B6%E5%88%86%E6%9E%90/)

## Javassist

- [Getting Started with Javassist](https://www.javassist.org/tutorial/tutorial.html)

## Shadeæä»¶

- `Maven`ç[Shade](http://maven.apache.org/plugins/maven-shade-plugin/)æä»¶
