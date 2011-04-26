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
  '((1 c c c c )
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

(defmacro single-method
  "Returns a proxied or reified instance of c, which must be a class
  or interface with exactly one method. Forwards method calls to the
  function f, which must accept the same number of arguments as the
  method, not including the 'this' argument.

  If c is a class, not an interface, then it must have a public,
  no-argument constructor."
  [c f]
  {:pre [(symbol? c)]}
  (let [klass (resolve c)]
    (assert (instance? java.lang.Class klass))
    (let [methods (.getDeclaredMethods klass)]
      (assert (= 1 (count methods)))
      (let [method (first methods)
            method-name (symbol (.getName method))
            arg-count (count (.getParameterTypes method))
            args (repeatedly arg-count gensym)]
        (if (.isInterface klass)
          `(let [f# ~f]
             (reify ~c (~method-name ~(vec (cons (gensym "this") args))
                                     (f# ~@args))))
          `(let [f# ~f]
             (proxy [~c] [] (~method-name ~(vec args) (f# ~@args)))))))))

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

(defn open-image-frame [icon]
  (.add @game-board
        (let [f (borderless-internal-frame)]
          (doto f
            (.setSize 200 200)
            (.setLocation 0 0)
            (.add (JLabel. icon))
            (.setVisible true)
            (.moveToFront)
            (.addMouseListener 
             (proxy [MouseAdapter] []
               (mousePressed [e]
                 (-> f (.getDesktopPane) (.getDesktopManager) (.beginDraggingFrame f)))
               (mouseReleased [e]
                 (-> f (.getDesktopPane) (.getDesktopManager) (.endDraggingFrame f)))))))))


(defn open-image [path]
  (->
   (open-or-create path)
   (ImageIO/read)
   (.getScaledInstance 200 200 0)
   (ImageIcon.)
   (JLabel.)))

(def open-image (memoize open-image))

(defn image-from-list [list point]
  (-> list
      (.getModel)
      (.getElementAt (.locationToIndex list point))
      (.getIcon)))

(defn images-list [directory]
  (let [files (.listFiles (File. directory))
        images (map open-image files)]
    (JScrollPane.
     (doto (JList. (to-array images))
       (.setCellRenderer (single-method ListCellRenderer (fn [_ value _ _ _] value)))
       (.addMouseListener
        (proxy [MouseAdapter] []
          (mousePressed [e]
            (let [f (open-image-frame (image-from-list (.getSource e) (.getPoint e)))]
              (doto f
                (.addMouseMotionListener
                 (proxy [MouseMotionAdapter] []
                   (mouseDragged [e]
                     (let [pos  (.getLocation f)
                           p (SwingUtilities/convertPoint (.getSource e) (.getX e) (.getY e) (.getDesktopPane f))]
                       (when (or (> 200 (rem (.getX p) 200))
                                 (> 200 (rem (.getY p) 200)))
                         (let [y (- (.getY p) (rem (.getY p) 200))
                               x (- (.getX p) (rem (.getX p) 200))]
                           (-> f (.getDesktopPane) (.getDesktopManager) (.dragFrame f x y))))))))
                ))
            )))))))

(defn game-board-panel []
  (let [gb (doto (JDesktopPane.) (.setBackground Color/white))]
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