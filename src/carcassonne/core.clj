(ns carcassonne.core
  (:import [javax.swing JFrame JPanel JLabel
            TransferHandler
            ImageIcon JList ListModel ListCellRenderer
            SwingUtilities JButton JDesktopPane JInternalFrame
            JScrollPane UIManager]
           [java.awt.event MouseAdapter MouseMotionListener]
           [java.awt Color]
           [javax.imageio ImageIO]
           [java.io File]
           [net.miginfocom.swing MigLayout]))

(def cards
  '((1 c c c c)
    (1 r r r r)
    (4 r r r f)
    (3 r r r c)
    (4 f f f c)
    (3 c r r f)
    (3 c f r r)
    (3 c f c f c)
    (3 c f c f f)
    (5 c c r r)
    (2 c c f f f)
    (5 c c f f c)
    (4 c c c f)
    (3 c c c r)
    (3 f f f f m)
    (2 f f f r m)
    (8 f r f r)
    (3 f r c r)
    (9 f f r r)))

(defn open-or-create [file]
  (if (string? file)
    (File. file)
    file))

(def open-frame (agent nil))
(def game-board (agent nil))

(defn open-image-frame [pos icon]
  (let [frame 
        (doto (JInternalFrame.)
          (.setSize 200 200)
          (.setLocation 0 0)
          (.add (JLabel. icon))
          (.setVisible true)
          (.moveToFront)])
    (send open-frame (fn [x] frame))
    (println "creating " frame)
    (.add @game-board frame)))

(defn close-open-frame []
  (let [f @open-frame]
    (when f
      (.setVisible f false)
      (send open-frame (fn [x] nil)))))

(defn open-image [path]
  (let [icon (ImageIcon.
              (.getScaledInstance
               (ImageIO/read (open-or-create path)) 200 200 0))]
    (JLabel. icon)))

(def open-image (memoize open-image))

(defn images-list [directory]
  (let [files (.listFiles (File. directory))
        images (map open-image files)]
    (JScrollPane.
     (doto (JList. (to-array images))
       (.setCellRenderer
        (proxy [ListCellRenderer] []
          (getListCellRendererComponent [list value index isSelected cellHasFocus]
            value)))
       (.addMouseListener
        (proxy [MouseAdapter] []
          (mousePressed [e]
            (println "pressed")
            (let [list (.getSource e)
                  p (.getPoint e)
                  index (.locationToIndex list p)
                  model (.getModel list)
                  image (.getElementAt model index)]
              (open-image-frame p (.getIcon image))))))))))

(defn game-board-panel []
  (let [gb (JDesktopPane.)]
    (doto gb
      (.setBackground Color/white))
    (send game-board (fn [x] gb))
    gb))


(defn open-window []
  (doto (JFrame. "Carcassonne")
    (.setSize 1000 750)
    (.addMouseMotionListener
     (proxy [MouseMotionListener] []
       (mouseDragged [e]
         (let [f @open-frame]
           (when f
             (.setLocation f (.getPos e)))))
       (mouseMoved [e]
         nil)))
    (.add
     (doto (JPanel.)
       (.setLayout (MigLayout. ))
       (.add (images-list "/home/nathan/Projects/carcassonne/pictures/finished"))
       (.add (game-board-panel) "grow, push, wrap")))
    (.setVisible true)))

(defn -main []
  (SwingUtilities/invokeLater open-window))
