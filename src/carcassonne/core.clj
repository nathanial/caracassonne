(ns carcassonne.core
  (:import [javax.swing JFrame JPanel JLabel
            TransferHandler
            ImageIcon JList ListModel ListCellRenderer
            SwingUtilities JButton JDesktopPane JInternalFrame
            JScrollPane UIManager]
           [javax.swing.plaf InternalFrameUI]
           [java.awt.event MouseAdapter MouseMotionListener MouseMotionAdapter]
           [java.awt Color Dimension]
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

(def game-board (agent nil))

(defn borderless-internal-frame []
  (let [f (JInternalFrame. )
        ui (.getUI f)]
    (doto f
      (.setBorder nil))
    (doto ui
      (.. (getNorthPane) (setPreferredSize (Dimension. 0 0))))
    f))

(defn open-image-frame [pos icon]
  (let [frame 
        (doto (borderless-internal-frame)
          (.setSize 200 200)
          (.setLocation 0 0)
          (.add (JLabel. icon))
          (.setVisible true)
          (.moveToFront))]
    (.add @game-board frame)))

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
              (let [f (open-image-frame p (.getIcon image))]
                (doto f
                  (.addMouseListener 
                   (proxy [MouseAdapter] []
                     (mousePressed [e]
                       (let [dm (.. f (getDesktopPane) (getDesktopManager))]
                         (.beginDraggingFrame dm f)
                         (println "began dragging")))
                     (mouseReleased [e]
                       (let [dm (.. f (getDesktopPane) (getDesktopManager))]
                         (.endDraggingFrame dm f)
                         (println "ended dragging")))))

                  (.addMouseMotionListener
                   (proxy [MouseMotionAdapter] []
                     (mouseDragged [e]
                       (let [dm (.. f (getDesktopPane) (getDesktopManager))
                             pos  (.getLocation f)
                             p (SwingUtilities/convertPoint (.getSource e) (.getX e) (.getY e) (.getDesktopPane f))
                             dx (- (. pos x) (.getX e))
                             dy (- (. pos y) (.getY e))]
                         (when (> 50 (rem (.getX p) 200))
                           (.dragFrame dm f (- (.getX p) 100) (- (.getY p) 100)))
))))))
              ))))))))

(defn game-board-panel []
  (let [gb (JDesktopPane.)]
    (doto gb
      (.setBackground Color/white))
    (send game-board (fn [x] gb))
    gb))


(defn open-window []
  (doto (JFrame. "Carcassonne")
    (.setSize 1000 750)
    (.add
     (doto (JPanel.)
       (.setLayout (MigLayout. ))
       (.add (images-list "/home/nathan/Projects/carcassonne/pictures/finished"))
       (.add (game-board-panel) "grow, push, wrap")))
    (.setVisible true)))

(defn -main []
  (SwingUtilities/invokeLater open-window))