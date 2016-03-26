package gameoftetris;

import java.util.Arrays;

// Features being used are:
// 1. Height sum
// 2. Number of holes
// 3. Completed lines
// 4. Height variation (between adjacent columns)
// 5. Terminal i.e. lost state
public class PlayerSkeleton {

    // public static double HEIGHT_SUM_WEIGHT = 0.51f;
    public static double NUM_HOLES_WEIGHT = 2377.641f;
    public static double COMPLETE_LINES_WEIGHT = 150.956056f;
    public static double HEIGHT_VAR_WEIGHT = 327.15828f;
    public static double LOST_WEIGHT = 943.2513f;
    public static double MAX_HEIGHT_WEIGHT = 155.662536f;
    public static final double pitDepthsWeight = 614.81148f;
    public static final double meanHeightDifferenceWeight = 513.80154f;

    public static class TestState {
        int[][] field;
        int[] top;
        int turn;
        int rowsCleared;
        boolean lost = false;

        public TestState(State s) {
            this.field = cloneField(s.getField());
            this.top = Arrays.copyOf(s.getTop(), s.getTop().length);
            this.turn = s.getTurnNumber();
            this.rowsCleared = s.getRowsCleared();
        }

        public TestState(TestState s) {
            this.field = cloneField(s.field);
            this.top = Arrays.copyOf(s.top, s.top.length);
            this.turn = s.turn;
            this.rowsCleared = s.rowsCleared;
        }

        private int[][] cloneField(int[][] field) {
            int[][] newField = new int[field.length][];
            for (int i = 0; i < newField.length; i++) {
                newField[i] = Arrays.copyOf(field[i], field[i].length);
            }
            return newField;
        }

        // returns false if you lose - true otherwise
        public boolean makeMove(int piece, int orient, int slot) {
            // height if the first column makes contact
            int height = top[slot] - pBottom[piece][orient][0];
            // for each column beyond the first in the piece
            for (int c = 1; c < pWidth[piece][orient]; c++) {
                height = Math.max(height, top[slot + c]
                    - pBottom[piece][orient][c]);
            }

            // check if game ended
            if (height + pHeight[piece][orient] >= ROWS) {
                lost = true;
                return false;
            }

            // for each column in the piece - fill in the appropriate blocks
            for (int i = 0; i < pWidth[piece][orient]; i++) {

                // from bottom to top of brick
                for (int h = height + pBottom[piece][orient][i]; h < height
                    + pTop[piece][orient][i]; h++) {
                    field[h][i + slot] = turn;
                }
            }

            // adjust top
            for (int c = 0; c < pWidth[piece][orient]; c++) {
                top[slot + c] = height + pTop[piece][orient][c];
            }

            int rowsCleared = 0;

            // check for full rows - starting at the top
            for (int r = height + pHeight[piece][orient] - 1; r >= height; r--) {
                // check all columns in the row
                boolean full = true;
                for (int c = 0; c < COLS; c++) {
                    if (field[r][c] == 0) {
                        full = false;
                        break;
                    }
                }
                // if the row was full - remove it and slide above stuff down
                if (full) {
                    rowsCleared++;
                    // for each column
                    for (int c = 0; c < COLS; c++) {

                        // slide down all bricks
                        for (int i = r; i < top[c]; i++) {
                            field[i][c] = field[i + 1][c];
                        }
                        // lower the top
                        top[c]--;
                        while (top[c] >= 1 && field[top[c] - 1][c] == 0)
                            top[c]--;
                    }
                }
            }
            return true;
        }

    }

    // implement this function to have a working system
    public int pickMove(State s, int[][] legalMoves) {
        // Explore legalMoves.length new states
        // legalMoves: an array of n total possible moves
        // each one of n moves contain orientation as index 0 and slot as index
        // 1
        double bestValueSoFar = -1;
        TestState bestStateSoFar = null;
        int bestMoveSoFar = 0;
        for (int i = 0; i < legalMoves.length; i++) {
            TestState state = new TestState(s);
            state.makeMove(s.nextPiece, legalMoves[i][ORIENT],
                legalMoves[i][SLOT]);
            // double value = !state.lost ? evaluateState(state)
            // : evaluateOneLevelLower(state);
            double value = evaluateOneLevelLower(state);
            if (value > bestValueSoFar || bestStateSoFar == null) {
                bestStateSoFar = state;
                bestValueSoFar = value;
                bestMoveSoFar = i;
            }

        }
        return bestMoveSoFar;
    }

    private double evaluateState(TestState state) {
        double sumLowerLevel = 0;
        int numMoves = 0;
        for (int i = 0; i < N_PIECES; i++) {
            for (int j = 0; j < legalMoves[i].length; j++) {
                TestState lowerState = new TestState(state);
                lowerState.makeMove(i, legalMoves[i][j][ORIENT],
                    legalMoves[i][j][SLOT]);
                sumLowerLevel += evaluateOneLevelLower(lowerState);
                numMoves++;
            }
        }

        return sumLowerLevel / numMoves;
    }

    private double evaluateOneLevelLower(TestState state) {
        // Evaluate the state given features to be tested and weights

        double h = /*-heightSum(state) * HEIGHT_SUM_WEIGHT + */
        -numHoles(state) * NUM_HOLES_WEIGHT + numRowsCleared(state)
            * COMPLETE_LINES_WEIGHT + -heightVariationSum(state)
            * HEIGHT_VAR_WEIGHT + lostStateValue(state) * LOST_WEIGHT
            + -maxHeight(state) * MAX_HEIGHT_WEIGHT + -pitEval(state)
            * pitDepthsWeight + -meanHeightEval(state)
            * meanHeightDifferenceWeight;
        return h;
    }

    private int lostStateValue(TestState state) {
        return hasLost(state) ? -10 : 0;
    }

    private static int heightSum(TestState s) {
        int[] top = s.top;
        int sum = 0;
        for (int height : top) {
            sum += height;
        }

        return sum;
    }

    private static int maxHeight(TestState s) {
        int[] top = s.top;
        int maxSoFar = -1;
        for (int i : top) {
            maxSoFar = Math.max(maxSoFar, i);
        }

        return maxSoFar;
    }

    private static int numHoles(TestState s) {
        int[][] field = s.field;
        int sumHoles = 0;
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < s.top[col] - 1; row++) {
                if (field[row][col] == 0) {
                    sumHoles++;
                }
            }
        }
        return sumHoles;
    }

    private static int numRowsCleared(TestState s) {
        return s.rowsCleared;
    }

    private static int heightVariationSum(TestState s) {
        int[] top = s.top;
        int varSum = 0;
        for (int i = 0; i < top.length - 1; i++) {
            varSum += Math.abs(top[i] - top[i + 1]);
        }

        return varSum;
    }

    private static boolean hasLost(TestState s) {
        return s.lost;
    }

    // Depth of pits, a pit is a column with adjacent columns higher by at least
    // two blocks and the pit depth
    // is defined as the difference between the height of the pit column and the
    // shortest adjacent column.
    public double pitEval(TestState s) {
        int[] top = s.top;
        int sumOfPitDepths = 0;

        int pitHeight;
        int leftOfPitHeight;
        int rightOfPitHeight;

        // pit depth of first column
        pitHeight = top[0];
        rightOfPitHeight = top[1];
        int diff = rightOfPitHeight - pitHeight;
        if (diff > 2) {
            sumOfPitDepths += diff;
        }

        for (int col = 0; col < State.COLS - 2; col++) {
            leftOfPitHeight = top[col];
            pitHeight = top[col + 1];
            rightOfPitHeight = top[col + 2];

            int leftDiff = leftOfPitHeight - pitHeight;
            int rightDiff = rightOfPitHeight - pitHeight;
            int minDiff = leftDiff < rightDiff ? leftDiff : rightDiff;

            if (minDiff > 2) {
                sumOfPitDepths += minDiff;
            }
        }

        // pit depth of last column
        pitHeight = top[State.COLS - 1];
        leftOfPitHeight = top[State.COLS - 2];
        diff = leftOfPitHeight - pitHeight;
        if (diff > 2) {
            sumOfPitDepths += diff;
        }

        return sumOfPitDepths;

    }

    // Mean height difference, the average of the difference between the height
    // of each column and the mean height of the state.
    public double meanHeightEval(TestState s) {
        int[] top = s.top;

        int sum = 0;
        for (int height : top) {
            sum += height;
        }

        float meanHeight = (float) sum / top.length;

        float avgDiff = 0;
        for (int height : top) {
            avgDiff += Math.abs(meanHeight - height);
        }

        return avgDiff / top.length;
    }

    public static void main(String[] args) {

        State s = new State();
        new TFrame(s);
        double[] weights = {50.5361659424388847, 0.9309995852895347,
            1.9204180105088573, 0.800421901913813, 1.636926959676058,
            0.1517331363630292};
        PlayerSkeleton p = new PlayerSkeleton(weights);
        while (!s.lost) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
            s.draw();
            s.drawNext(0, 0);
        }
        System.out.println("You have completed " + s.getRowsCleared()
            + " rows.");
    }

    public PlayerSkeleton(double[] weights) {

    }

    public int run() {

        State s = new State();
        while (!s.lost) {
            s.makeMove(pickMove(s, s.legalMoves()));
        }
        System.out.println("You have completed " + s.getRowsCleared()
            + " rows.");

        return s.getRowsCleared();
    }

    public static final int COLS = State.COLS;
    public static final int ROWS = State.ROWS;
    public static final int N_PIECES = State.N_PIECES;
    // all legal moves - first index is piece type - then a list of 2-length
    // arrays
    protected static int[][][] legalMoves = new int[N_PIECES][][];

    // indices for legalMoves
    public static final int ORIENT = 0;
    public static final int SLOT = 1;

    // possible orientations for a given piece type
    protected static int[] pOrients = {1, 2, 4, 4, 4, 2, 2};

    // the next several arrays define the piece vocabulary in detail
    // width of the pieces [piece ID][orientation]
    protected static int[][] pWidth = { {2}, {1, 4}, {2, 3, 2, 3},
        {2, 3, 2, 3}, {2, 3, 2, 3}, {3, 2}, {3, 2}};
    // height of the pieces [piece ID][orientation]
    private static int[][] pHeight = { {2}, // square
        {4, 1}, // vertical piece
        {3, 2, 3, 2}, // L
        {3, 2, 3, 2}, //
        {3, 2, 3, 2}, // T
        {2, 3}, {2, 3}};
    private static int[][][] pBottom = {
        {{0, 0}},
        { {0}, {0, 0, 0, 0}},
        { {0, 0}, {0, 1, 1}, {2, 0}, {0, 0, 0}}, // L,
        { {0, 0}, {0, 0, 0}, {0, 2}, {1, 1, 0}},
        { {0, 1}, {1, 0, 1}, {1, 0}, {0, 0, 0}}, { {0, 0, 1}, {1, 0}},
        { {1, 0, 0}, {0, 1}}};
    private static int[][][] pTop = { {{2, 2}}, { {4}, {1, 1, 1, 1}},
        { {3, 1}, {2, 2, 2}, {3, 3}, {1, 1, 2}},
        { {1, 3}, {2, 1, 1}, {3, 3}, {2, 2, 2}},
        { {3, 2}, {2, 2, 2}, {2, 3}, {1, 2, 1}}, { {1, 2, 2}, {3, 2}},
        { {2, 2, 1}, {2, 3}}};

    // initialize legalMoves
    // legalMoves[piece type][num legal moves][tuple of orient and slot]
    {
        // for each piece type
        for (int i = 0; i < N_PIECES; i++) {
            // figure number of legal moves
            int n = 0;
            for (int j = 0; j < pOrients[i]; j++) {
                // number of locations in this orientation
                n += COLS + 1 - pWidth[i][j];
            }
            // allocate space
            legalMoves[i] = new int[n][2];
            // for each orientation
            n = 0;
            for (int j = 0; j < pOrients[i]; j++) {
                // for each slot
                for (int k = 0; k < COLS + 1 - pWidth[i][j]; k++) {
                    legalMoves[i][n][ORIENT] = j;
                    legalMoves[i][n][SLOT] = k;
                    n++;
                }
            }
        }
    }

}
