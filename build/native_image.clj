(ns native-image
  (:import [com.gluonhq.substrate SubstrateDispatcher ProjectConfiguration]
           [java.nio.file Path]
           [com.gluonhq.substrate.model Triplet]))

(defn- path ^Path [& [str & strs]]
  (Path/of str (into-array String strs)))

;; NOTES:
;; You need to use gluon's fork of graalvm, you can find the links on this page:
;; https://github.com/gluonhq/client-samples
;; On linux you might need to install additional packages:
;; https://github.com/gluonhq/substrate#linux-hosts
;; how to use it:
;; 1. run `clj -A:build:compile` to compile clojure code to `classes` directory
;; 2. run `clj -A:uberjar` to create `dist/hn.jar` from classes in `classes` directory
;; 3. run `clj -A:build:native-image` to run this file
;; Currently compilation fails with the following exception:
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] Fatal error:com.oracle.svm.core.util.VMError$HostedError: com.oracle.svm.core.util.UserError$UserException: Static field or an object referenced from a static field changed during native image generation?
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]   object:java.lang.ref.SoftReference@1df10d99  of class: java.lang.ref.SoftReference
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]   reachable through:
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]     object: [Ljava.lang.ref.SoftReference;@68e4b433  of class: java.lang.ref.SoftReference[]
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]     object: Form(Object,Object,int,Object)void  of class: java.lang.invoke.MethodTypeForm
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]     object: (MethodHandle,Object,int,Object)void  of class: java.lang.invoke.MethodType
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]     object: MethodHandle(MethodHandle,Object,int,Object)void  of class: java.lang.invoke.DirectMethodHandle
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]     object: [Ljava.lang.invoke.MethodHandle;@791fe1f0  of class: java.lang.invoke.MethodHandle[]
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]     object: Invokers(Object,int,Object)void  of class: java.lang.invoke.Invokers
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]     object: (Object,int,Object)void  of class: java.lang.invoke.MethodType
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]     root: java.lang.invoke.MethodHandleImpl$AsVarargsCollector.invokeWithArguments(Object[])
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB]
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] 	at com.oracle.svm.core.util.VMError.shouldNotReachHere(VMError.java:72)
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] 	at com.oracle.svm.hosted.NativeImageGenerator.doRun(NativeImageGenerator.java:648)
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] 	at com.oracle.svm.hosted.NativeImageGenerator.lambda$run$0(NativeImageGenerator.java:451)
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] 	at java.base/java.util.concurrent.ForkJoinTask$AdaptedRunnableAction.exec(ForkJoinTask.java:1407)
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] 	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] 	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] 	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] 	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)
;; [Thu Apr 02 08:58:24 CEST 2020][INFO] [SUB] 	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:177)

(defn -main []
  (.nativeCompile
    (SubstrateDispatcher.
      (.toAbsolutePath (path "native-build"))
      (doto (ProjectConfiguration. "hn.core" (str (.toAbsolutePath (path "dist" "hn.jar"))))
        (.setGraalPath (path "/opt/graalvm-svm-linux-20.1.0-ea+26"))
        (.setAppId "hn")
        (.setAppName "hn")
        (.setTarget (Triplet/fromCurrentOS))))))
