(ns table-widget.core
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET]]))

(defn add-total [data]
  (map #(update % :total + (:gold %) (:silver %) (:bronze %)) data))

(defn fetch-link! [data]
  (GET "https://s3-us-west-2.amazonaws.com/reuters.medals-widget/medals.json"
    {:response-format :json
     :keywords? true
     :handler #(reset! data {:data (add-total %)})
     :error-handler (fn [{:keys [status status-text]}]
                      (reset! data {:error (str status " " status-text)}))}))

(defn get-sort-order [sort-val]
  (case sort-val
    :gold (juxt :gold :silver)
    :silver (juxt :silver :gold)
    :bronze (juxt :bronze :gold)
    :total (juxt :total :gold)))

(defn sorted-contents [data sorting]
  (sort-by (get-sort-order sorting) #(compare %2 %1) data))

(defn draw-table [default-sorting]
  (let [data (atom nil)
        sorting (atom (if (nil? default-sorting) :gold (keyword default-sorting)))]
    (fetch-link! data)
    (fn []
      [:div {:class "medal-table-wrapper"}
       [:table {:class "medal-table"}
        [:caption (str "MEDAL COUNT")]
        [:thead
         ^{:key "title-row"}
         [:tr {:class "header-tr"}
          [:th " "]
          [:th " "]
          [:th " "]
          [:th {:on-click #(reset! sorting :gold) :class (if (= @sorting :gold) "active" nil)} [:div {:class "circle gold"}]]
          [:th {:on-click #(reset! sorting :silver) :class (if (= @sorting :silver) "active" nil)} [:div {:class "circle silver"}]]
          [:th {:on-click #(reset! sorting :bronze) :class (if (= @sorting :bronze) "active" nil)} [:div {:class "circle bronze"}]]
          [:th {:on-click #(reset! sorting :total) :class (str (if (= @sorting :total) "active" nil) " total-th")} "TOTAL"]]]
        [:tbody
         (if (:error @data)
           [:tr [:td {:colSpan 7} (str "Something went wrong: " (:error @data))]]
           nil)
         (for [[index country] (map-indexed vector (take 10 (sorted-contents (:data @data) @sorting)))]
           ^{:key (:code country)}
           [:tr
            [:td (+ index 1)] ;; number
            [:td [:div {:class (str "flag " "flag-" (:code country))}]] ;; flag 
            [:td {:class "code-td"} (:code country)]
            [:td (:gold country)]
            [:td (:silver country)]
            [:td (:bronze country)]
            [:td {:class "total-td"} (:total country)]])]]])))

(defn start [div-id default-sorting]
  (js/console.log div-id default-sorting)
  (r/render-component [draw-table default-sorting]
                      (. js/document (getElementById div-id))))

(defn ^:export init [div-id default-sorting]
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start div-id default-sorting))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
