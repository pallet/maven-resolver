(ns com.palletops.maven-resolver
  "Resolve maven dependencies with aether."
  (:refer-clojure :exclude [resolve])
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :refer [debugf tracef]]
   [cemerick.pomegranate.aether :as aether :refer :all])
  (:import
   [org.sonatype.aether.util.artifact ArtifactProperties DefaultArtifact]
   org.sonatype.aether.resolution.ArtifactRequest))

;;; # Wagon Registration
;;; Register known wagons if they are on the classpath
(defmacro register-if-available
  "Register the specified scheme using the wagon class if it is on the
  classpath."
  [scheme class-sym]
  (if-let [class (try (Class/forName (str class-sym))
                      (catch ClassNotFoundException _))]
    `(let [scheme# ~scheme]
       (cemerick.pomegranate.aether/register-wagon-factory!
        scheme# #(new ~class-sym))
       (debugf "Registered wagon for scheme %s using %s"
               scheme# ~(str class-sym)))
    (tracef "Not registering wagon for scheme %s. %s not on classpath."
            scheme ~(str class-sym))))

(register-if-available
 "s3" org.springframework.aws.maven.SimpleStorageServiceWagon)
(register-if-available
 "s3p" org.springframework.aws.maven.PrivateS3Wagon)

;;; # Basic Artifact Resolution
;;; This should belong in pomegranate.
(defn artifact
  [[group-artifact version & {:keys [scope optional exclusions]
                              :as opts
                              :or {scope "compile"
                                   optional false}}
    :as dep-spec]]
  (DefaultArtifact. (#'aether/coordinate-string dep-spec)))


(defn resolve-artifacts*
  "Resolves artifacts for the coordinates kwarg, using repositories from the
`:repositories` kwarg.

If you don't want to mess with the Aether implmeentation classes, then use
`resolve-artifacts` instead.

Options are as for pomegranate's
[`resolve-dependencies*`](https://github.com/cemerick/pomegranate/blob/master/src/main/clojure/cemerick/pomegranate/aether.clj#L484)"
  [& {:keys [repositories coordinates files retrieve local-repo
             transfer-listener offline? proxy mirrors repository-session-fn]
      :or {retrieve true}}]
  (debugf "resolve-artifacts* %s" (pr-str coordinates))
  (let [repositories (or repositories maven-central)
        system (#'aether/repository-system)
        mirror-selector-fn (memoize (partial #'aether/mirror-selector-fn mirrors))
        mirror-selector (#'aether/mirror-selector mirror-selector-fn proxy)
        session ((or repository-session-fn
                     repository-session)
                 {:repository-system system
                  :local-repo local-repo
                  :offline? offline?
                  :transfer-listener transfer-listener
                  :mirror-selector mirror-selector})
        deps (->> coordinates
                  (map #(if-let [local-file (get files %)]
                          (.setArtifact
                           (artifact %)
                           (-> (artifact %)
                               .getArtifact
                               (.setProperties
                                {ArtifactProperties/LOCAL_PATH
                                 (.getPath (io/file local-file))})))
                          (artifact %)))
                  vec)
        repositories (vec (map #(let [repo (#'aether/make-repository % proxy)]
                                  (-> session
                                      (.getMirrorSelector)
                                      (.getMirror repo)
                                      (or repo)))
                      repositories))]
    (debugf "deps %s" (pr-str deps))

    (doall
     (for [dep deps]
       (.resolveArtifact
        system session (ArtifactRequest. dep repositories nil))))))

(defn resolve-artifacts
  "Same as `resolve-artifacts*`, but returns a sequence of files on disk. "
  [& args]
  (->> (apply resolve-artifacts* args)
       (mapv #(.getArtifact %))
       (mapv #(.getFile %))))

;;; # Resolve Artifacts Coordinates

(defn resolve-coordinates
  "Resolve a sequence of coordinate vectors into a local repository, and return
the local file path.

`:local-repo`
: a path to locate the local repository to resolve into.

`:repositories`
: a map of leiningen style repository definitions."
  [coordinates {:keys [repositories local-repo] :as options}]
  (debugf "resolve %s %s" coordinates options)
  (apply
   resolve-artifacts
   :coordinates coordinates
   :retrieve true
   (apply concat options)))
