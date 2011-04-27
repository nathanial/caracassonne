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
  '((1 c c c c "IMAG0080.jpg")
    (1 r r r r "IMAG0081.jpg")
    (4 r r r f "IMAG0087.jpg")
    (3 r r r c "IMAG0091.jpg")
    (4 f f f c "IMAG0083.jpg")
    (3 c r r f "IMAG0082.jpg")
    (3 c f r r "IMAG0089.jpg")
    (3 c f c f c "IMAG0105.jpg")
    (3 c f c f f "IMAG0107.jpg")
    (5 c c r r "IMAG0103.jpg")
    (2 c c f f f "IMAG0110.jpg")
    (5 c c f f c "IMAG0108.jpg")
    (4 c c c f "IMAG0095.jpg")
    (3 c c c r "IMAG0096.jpg")
    (3 f f f f m "IMAG0090.jpg")
    (2 f f f r m "IMAG0088.jpg")
    (8 f r f r "IMAG0093.jpg")
    (3 f r c r "IMAG0101.jpg")
    (9 f f r r "IMAG0098.jpg")))

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
                 (.. f (getDesktopPane) (getDesktopManager) (beginDraggingFrame f)))
               (mouseReleased [e]
                 (.. f (getDesktopPane) (getDesktopManager) (endDraggingFrame f)))))))))


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

(defn drag-to-square [image-frame mouse-event]
  (let [f image-frame
        e mouse-event]
    (let [p (SwingUtilities/convertPoint (.getSource e) (.getX e) (.getY e) (.getDesktopPane f))]
      (when (or (> 200 (rem (.getX p) 200))
                (> 200 (rem (.getY p) 200)))
        (let [y (- (.getY p) (rem (.getY p) 200))
              x (- (.getX p) (rem (.getX p) 200))]
          (.. f (getDesktopPane) (getDesktopManager) (dragFrame f x y)))))))

(defn mouse-dragged [action]
  (proxy [MouseMotionAdapter] []
    (mouseDragged [e] (action e))))

(defn mouse-pressed [action]
  (proxy [MouseAdapter] []
    (mousePressed [e]
      (action e))))

(defn images-list []
  (let [dir "/home/nathan/Projects/carcassonne/pictures/finished/"
        path #(str dir (last %))
        to-image #(open-image (File. (path %)))
        images (map #(list %1 (to-image %1)) cards)]
    (JScrollPane.
     (doto (JList. (to-array (map last images)))
       (.setCellRenderer (single-method ListCellRenderer (fn [_ value _ _ _] value)))
       (.addMouseListener
        (mouse-pressed
         (fn [e]
           (let [f (open-image-frame (image-from-list (.getSource e) (.getPoint e)))]
             (.addMouseMotionListener f (mouse-dragged #(drag-to-square f %)))))))))))

(defn open-window []
  (doto (JFrame. "Carcassonne")
    (.setSize 1000 750)
    (.add
     (doto (JPanel.)
       (.setLayout (MigLayout. ))
       (.add (images-list))
       (.add (game-board-panel) "grow, push, wrap")))
    (.setVisible true)))

(defn -main []
  (SwingUtilities/invokeLater open-window))