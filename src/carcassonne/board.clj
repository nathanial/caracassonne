(ns carcassonne.board
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

(def grid (atom []))

(defn game-board-panel []
  (let [gb (doto (JDesktopPane.) (.setBackground Color/white))]
    (send game-board (fn [x] gb))
    gb))
