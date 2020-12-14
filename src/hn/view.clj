(ns hn.view
  (:require [clojure.string :as str]
            [hn.event :as event]
            [cljfx.css :as css]
            [cljfx.api :as fx]
            [cljfx.composite :as fx.composite]
            [cljfx.lifecycle :as fx.lifecycle]
            [cljfx.fx.list-cell :as fx.list-cell]
            [crouton.html :as html])
  (:import [java.time.format DateTimeFormatter]
           [java.time ZoneId Instant]
           [java.net URI]
           [javafx.scene.control ListView ListCell]
           [javafx.util Callback]))

(def style
  (let [font "'sans-serif'"
        separator-color "#bbb"
        default-spacing 10
        small-spacing 5
        scroll-bar-breadth 15]
    (css/register ::style
      {".hn" {:-fx-background-color "#f6f6ef"
              "-info" {"-panel" {:-fx-spacing 8}
                       "-label" {:-fx-font-family font
                                 :-fx-font-size 12
                                 :-fx-text-fill :grey}
                       "-text" {:-fx-font-family font
                                :-fx-font-size 12
                                :-fx-fill :grey}
                       "-separator" {:-fx-min-width 1
                                     :-fx-background-color separator-color}}
              "-title" {:-fx-font-family font
                        :-fx-font-size 16}
              "-text" {:-fx-font-family font
                       :-fx-font-size 13}
              "-italic" {:-fx-font-style :italic}
              "-underline-on-hover:hover" {:-fx-underline true}
              "-headline" {:-fx-spacing default-spacing
                           :-fx-padding default-spacing
                           :-fx-alignment :baseline-left}
              "-button" {:-fx-border-width [1 1 2 1]
                         :-fx-font-family font
                         :-fx-border-color :grey
                         :-fx-border-radius 5
                         :-fx-background-color "#eee"
                         :-fx-padding [small-spacing default-spacing]
                         ":hover" {:-fx-background-color :transparent}}
              "-split-pane>.split-pane-divider" {:-fx-background-color separator-color
                                                 :-fx-background-insets [0 10.5]
                                                 :-fx-padding [0 11]}

              "-comment" {"-cell" {:-fx-padding [small-spacing default-spacing]}}
              "-stories" {"-item" {:-fx-spacing small-spacing}
                          "-cell" {:-fx-padding [small-spacing default-spacing]}}}
       ".error" {:-fx-text-fill :red}
       ".list-cell:empty" {:-fx-background-color :transparent}
       ".scroll-bar" {:-fx-background-color :transparent
                      ":vertical" {"> .increment-button > .increment-arrow"
                                   {:-fx-pref-width scroll-bar-breadth}
                                   "> .decrement-button > .decrement-arrow"
                                   {:-fx-pref-width scroll-bar-breadth}}
                      "> .decrement-button" {:-fx-padding 0
                                             "> .decrement-arrow" {:-fx-shape nil
                                                                   :-fx-padding 0}}
                      "> .increment-button" {:-fx-padding 0
                                             "> .increment-arrow" {:-fx-shape nil
                                                                   :-fx-padding 0}}
                      "> .thumb" {:-fx-background-color :grey
                                  :-fx-background-radius scroll-bar-breadth
                                  ":pressed" {:-fx-background-color :black}}}})))

(def ^:private ^DateTimeFormatter date-time-formatter
  (-> (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm")
      (.withZone (ZoneId/systemDefault))))

(defn- info-panel [props]
  (-> props
      (assoc :fx/type :h-box
             :style-class "hn-info-panel")
      (update :children
              #(interpose {:fx/type :region :style-class "hn-info-separator"} %))))

(defn- info-label [props]
  (assoc props :fx/type :label
               :style-class "hn-info-label"))

(defn- timestamp [{:keys [time]}]
  {:fx/type info-label
   :text (.format date-time-formatter (Instant/ofEpochSecond time))})

(defn footer [{:keys [story]}]
  {:fx/type info-panel
   :children [{:fx/type info-label
               :text (str (:score story) " points")}
              {:fx/type info-label
               :text (str "by " (:by story))}
              {:fx/type timestamp
               :time (:time story)}
              {:fx/type info-label
               :text (str (:descendants story) " comments")}]})

(defn- create-cell-factory [f]
  (reify Callback
    (call [_ _]
      (let [*props (volatile! {})]
        (proxy [ListCell] []
          (updateItem [item empty]
            (let [^ListCell this this]
              (proxy-super updateItem item empty)
              (vswap! *props f this item empty))))))))

(def ext-with-list-cell-factory
  (fx/make-ext-with-props
    (fx.composite/props ListView
      :cell-factory [:setter (fx.lifecycle/detached-prop-map fx.list-cell/props)
                     :coerce create-cell-factory])))

(defn stories [{:keys [stories items error]}]
  {:fx/type :v-box
   :children
   [{:fx/type :h-box
     :style-class "hn-headline"
     :children [{:fx/type :button
                 :style-class "hn-button"
                 :on-action {::event/type ::event/load-stories}
                 :text "⟲"}
                {:fx/type :label
                 :style-class "hn-title"
                 :text "Hacker News"}
                {:fx/type :label
                 :style-class "error"
                 :text error}]}
    {:fx/type ext-with-list-cell-factory
     :v-box/vgrow :always
     :props
     {:cell-factory
      (fn [[index id]]
        {:style-class "hn-stories-cell"
         :content-display :graphic-only
         :graphic
         {:fx/type :stack-pane
          :min-width 0
          :pref-width 1
          :children
          [{:fx/type :h-box
            :spacing 5
            :children
            [{:fx/type :v-box
              :min-width 28
              :alignment :top-right
              :children [{:fx/type info-label
                          :min-width :use-pref-size
                          :text (str (inc index) ".")}]}
             {:fx/type :v-box
              :h-box/hgrow :always
              :style-class "hn-stories-item-body"
              :children
              (if-let [story (get items id)]
                [{:fx/type :text-flow
                  :children
                  (cond->
                    [{:fx/type :text
                      :on-mouse-clicked {::event/type ::event/open-story :id id}
                      :style-class ["hn-title" "hn-underline-on-hover"]
                      :text (:title story)}]
                    (:url story)
                    (conj {:fx/type :text
                           :style-class "hn-info-text"
                           :text (str " (" (.getAuthority (URI. (:url story))) ")")}))}
                 {:fx/type footer
                  :story story}]
                [{:fx/type info-label
                  :text "loading..."}])}]}]}})}
     :desc
     {:fx/type :list-view
      :style-class "hn-stories"
      :items (map-indexed vector stories)}}]})

(defn comment-tree [{:keys [items view]}]
  (let [[_ id] view
        walk (fn walk [ident id]
               (cons [ident id]
                     (when id
                       (mapcat #(walk (inc ident) %) (get-in items [id :kids])))))]
    {:fx/type ext-with-list-cell-factory
     :props
     {:cell-factory
      (fn [[ident id]]
        {:style-class "hn-comment-cell"
         :content-display :graphic-only
         :text ""
         :graphic
         {:fx/type :stack-pane
          :padding {:left (* ident 20)}
          :min-width 0
          :pref-width 1
          :min-height :use-pref-size
          :alignment :top-left
          :children
          [(if-let [comment (get items id)]
             (if (:deleted comment)
               {:fx/type info-label
                :text "deleted"}
               {:fx/type :v-box
                :children
                [{:fx/type info-panel
                  :children [{:fx/type info-label
                              :text (:by comment)}
                             {:fx/type timestamp
                              :time (:time comment)}]}
                 {:fx/type :text-flow
                  :children
                  (->> comment
                       :text
                       html/parse-string
                       :content
                       (some #(when (= :body (:tag %))
                                (:content %)))
                       (mapcat (fn parse-html [x]
                                 (cond
                                   (string? x)
                                   [{:fx/type :text
                                     :style-class "hn-text"
                                     :text x}]

                                   (= :p (:tag x))
                                   (into [{:fx/type :text
                                           :style-class "hn-text"
                                           :text "\n"}]
                                         (mapcat parse-html)
                                         (:content x))

                                   (= :a (:tag x))
                                   [{:fx/type :text
                                     :fill :blue
                                     :on-mouse-clicked {::event/type ::event/open-url
                                                        :url (-> x :attrs :href)}
                                     :style-class ["hn-text" "hn-underline-on-hover"]
                                     :text (str (first (:content x)))}]

                                   (= :i (:tag x))
                                   [{:fx/type :text
                                     :style-class ["hn-text" "hn-italic"]
                                     :text (str (first (:content x)))}]

                                   :else
                                   [{:fx/type :text
                                     :fill :red
                                     :style-class "hn-text"
                                     :text (str x)}]))))}]})
             {:fx/type info-label
              :text "loading..."})]}})}
     :desc
     {:fx/type :list-view
      :style-class "hn-comments"
      :items (mapcat #(walk 0 %) (get-in items [id :kids]))}}))

(defn story [{:keys [view items] :as state}]
  (let [[_ id] view
        story (get items id)
        left (when (:url story)
               {:fx/type :web-view
                :url (:url story)})
        right (when (pos? (count (:kids story)))
                (assoc state :fx/type comment-tree))]
    {:fx/type :v-box
     :style-class "hn-story"
     :children
     (-> [{:fx/type :h-box
           :style-class "hn-headline"
           :children [{:fx/type :button
                       :style-class "hn-button"
                       :text "←"
                       :on-action {::event/type ::event/open-stories}}
                      {:fx/type :v-box
                       :children [{:fx/type :label
                                   :style-class "hn-title"
                                   :wrap-text true
                                   :text (:title story)}
                                  {:fx/type footer
                                   :story story}]}]}]
         (cond-> (:text story)
                 (conj {:fx/type :label
                        :padding {:left 10 :right 10}
                        :style-class "hn-text"
                        :wrap-text true
                        :text (str/replace (:text story) "<p>" "\n\n")})

                 (and left right)
                 (conj {:fx/type :split-pane
                        :style-class "hn-split-pane"
                        :v-box/vgrow :always
                        :items [left right]})

                 (not= (some? left) (some? right))
                 (conj (assoc (or left right) :v-box/vgrow :always))))}))

(defn app [{:keys [view] :as state}]
  {:fx/type :stage
   :showing true
   :width 960
   :height 540
   :scene
   {:fx/type :scene
    :stylesheets [(::css/url style)]
    :root {:fx/type :anchor-pane
           :style-class "hn"
           :children [(assoc state
                        :fx/type (case (first view)
                                   :stories stories
                                   :story story)
                        :anchor-pane/bottom 0
                        :anchor-pane/right 0
                        :anchor-pane/left 0
                        :anchor-pane/top 0)]}}})