(ns juxt.crux-ui.frontend.views.header)


(defn root []
  [:header
   [:div.header__logo
      [:div.logo [:a {:href "/"} [:img.logo-img {:src "/static/img/console-logo.svg"}]]]]
   [:div.header__links
    [:a.header__links__item {:href "https://juxt.pro/crux/docs/index.html"}
     [:span.n "Documentation"]]
    [:a.header__links__item {:href "https://juxt-oss.zulipchat.com/#narrow/stream/194466-crux"}
     [:span.n "Community Chat"]]
    [:a.header__links__item {:href "mailto:crux@juxt.pro"}
     [:span.n "crux@juxt.pro"]]]])

