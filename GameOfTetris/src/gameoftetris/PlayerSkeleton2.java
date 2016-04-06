import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class PlayerSkeletonReference {
    private static final float numFaultsWeight = 6.0f;
    private static final float numRowsClearedWeight = 0.8f;
    private static final float roughnessWeight = 0.9f;
    private static final float hasLostWeight = 1;
    private static final float maxColumnHeightWeight = 0.8f;
    private static final float pitDepthsWeight = 1.2f;
    private static final float meanHeightDifferenceWeight = 0.85f;

    private MoveEvaluator evaluator;
    private MapReduce mapReduce;
    private ArrayList<Move> possibleMoves = new ArrayList<Move>();

    public static void main(String[] args) {
        State s = new State();

        ForkJoinPool executorService = new ForkJoinPool();
        PlayerSkeletonReference p = new PlayerSkeletonReference(executorService);

        new TFrame(s);
        try {
            while (!s.hasLost()) {
                s.makeMove(p.pickMove(s, s.legalMoves()));
                s.draw();
                s.drawNext(0, 0);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }

        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
    }

    public static final MoveEvaluator[] EVALUATORS;
    static {
        ArrayList<MoveEvaluator> evaluators = new ArrayList<MoveEvaluator>();

        // Column heights
        /*
         * for(int columnIndex = 0; columnIndex < State.COLS; ++columnIndex) {
         * evaluators.add(new ColumnHeight(columnIndex)); }
         */

        // Column height differences
        /*
         * for(int columnIndex = 0; columnIndex < State.COLS - 1; ++columnIndex)
         * { evaluators.add(new ColumnDiff(columnIndex, columnIndex + 1)); }
         */

        evaluators.add(new Roughness());
        evaluators.add(new MaxColumnHeight());
        evaluators.add(new NumRowsCleared());
        evaluators.add(new HasLost());
        evaluators.add(new NumFaults());
        // evaluators.add(new MeanHeight());
        // evaluators.add(new NumWells());
        evaluators.add(new PitDepths());
        evaluators.add(new MeanHeightDifference());

        EVALUATORS = evaluators.toArray(new MoveEvaluator[evaluators.size()]);
    }

    public PlayerSkeletonReference(ForkJoinPool forkJoinPool) {
        this.mapReduce = new MapReduce(forkJoinPool);
        float[] weights = new float[] {363.5092f, 194.57817f, 188.69507f, 943.2513f, 396.27356f, 512.3429f, 604.4724f};
        // { 587.5112f, 438.03345f, 474.9645f, 939.3418f, 408.60773f, 815.7669f
        // };
        this.evaluator = new WeightedSumEvaluator(EVALUATORS, weights);
    }

    public PlayerSkeletonReference(ForkJoinPool forkJoinPool, float[] weights) {
        this.mapReduce = new MapReduce(forkJoinPool);
        this.evaluator = new WeightedSumEvaluator(EVALUATORS, weights);
    }

    public int pickMove(State s, int[][] legalMoves) {
        int nextPiece = s.getNextPiece();
        ImmutableState currentState = new ImmutableState(s);
        return pickMove(currentState, nextPiece, legalMoves);
    }

    public int pickMove(ImmutableState currentState, int nextPiece, int[][] legalMoves) {
        possibleMoves.clear();
        for (int moveIndex = 0; moveIndex < legalMoves.length; ++moveIndex) {
            int orientation = legalMoves[moveIndex][0];
            int position = legalMoves[moveIndex][1];
            possibleMoves.add(new Move(currentState, moveIndex, nextPiece, orientation, position));
        }

        return mapReduce.mapReduce(EVAL_MOVE_FUNC, PICK_MOVE_FUNC, possibleMoves);
    }

    public static void printState(int[][] field) {
        for (int y = State.ROWS - 1; y >= 0; --y) {
            for (int x = 0; x < State.COLS; ++x) {
                System.out.print(field[y][x] != 0 ? '*' : '_');
            }
            System.out.println();
        }
        System.out.println("---");
    }

    private final MapFunc<Move, EvaluationResult> EVAL_MOVE_FUNC = new MapFunc<Move, EvaluationResult>() {
        @Override
        public EvaluationResult map(Move move) {
            ImmutableState state = move.getState();
            MoveResult moveResult = state.move(move.getPiece(), move.getOrientation(), move.getPosition());
            float score = evaluator.map(moveResult);
            return new EvaluationResult(move.getIndex(), score);
        }
    };

    private static final ReduceFunc<EvaluationResult, Integer> PICK_MOVE_FUNC =
        new ReduceFunc<EvaluationResult, Integer>() {
            @Override
            public Integer reduce(Iterable<EvaluationResult> results) {
                float maxScore = -Float.MAX_VALUE;
                int move = -1;

                for (EvaluationResult result : results) {
                    float score = result.getScore();
                    if (score > maxScore) {
                        maxScore = score;
                        move = result.getMove();
                    }
                }

                return move;
            }
        };

    // Nested classes because we are only allowed to use one file
    /**
     * An evaluator which uses a weighted sum of features as score
     */
    public static class WeightedSumEvaluator implements MoveEvaluator {
        public WeightedSumEvaluator(MoveEvaluator[] evaluators, float[] weights) {
            this.evaluators = evaluators;
            this.weights = weights;
        }

        @Override
        public Float map(MoveResult moveResult) {
            float sum = 0.0f;

            for (int i = 0; i < evaluators.length; ++i) {
                float score = evaluators[i].map(moveResult);
                sum += score * weights[i];
            }

            return sum;
        }

        private final MoveEvaluator[] evaluators;
        private final float[] weights;
    }

    /**
     * Doesn't do anything, just return 0 for testing purposes
     */
    public static class DummyEvaluator implements MoveEvaluator {
        @Override
        public Float map(MoveResult moveResult) {
            return 0.0f;
        }
    }

    // Not in use
    public static class NumWells implements MoveEvaluator {
        @Override
        public Float map(MoveResult moveResult) {
            int[] top = moveResult.getState().getTop();

            int numWells = 0;

            for (int column = 1; column < top.length - 1; ++column) {
                if (top[column - 1] < top[column] && top[column] < top[column + 1]) {
                    ++numWells;
                }
            }

            if (top[0] < top[1])
                ++numWells;
            if (top[top.length - 1] < top[top.length - 2])
                ++numWells;

            return -(float) numWells;
        }
    }

    // Not in use
    public static class DeepestWell implements MoveEvaluator {
        @Override
        public Float map(MoveResult moveResult) {
            int maxDepth = Integer.MIN_VALUE;
            int[] top = moveResult.getState().getTop();

            for (int column = 1; column < top.length - 1; ++column) {
                if (top[column - 1] < top[column] && top[column] < top[column + 1]) {
                    int depth = Math.max(top[column] - top[column - 1], top[column + 1] - top[column]);
                    maxDepth = Math.max(maxDepth, depth);
                }
            }

            if (top[0] < top[1]) {
                maxDepth = Math.max(maxDepth, top[1] - top[0]);
            }

            if (top[top.length - 1] < top[top.length - 2]) {
                maxDepth = Math.max(maxDepth, top[top.length - 2] - top[top.length - 1]);
            }

            return -(float) maxDepth;
        }
    }

    // Not in use
    public static class MeanHeight implements MoveEvaluator {
        @Override
        public Float map(MoveResult result) {
            int[] top = result.getState().getTop();

            int sum = 0;
            for (int height : top) {
                sum += height;
            }

            return -(float) sum / top.length;
        }
    }

    // Mean height difference, the average of the difference between the height
    // of each column and the mean height of the state.
    public static class MeanHeightDifference implements MoveEvaluator {
        @Override
        public Float map(MoveResult result) {
            int[] top = result.getState().getTop();

            int sum = 0;
            for (int height : top) {
                sum += height;
            }

            float meanHeight = (float) sum / top.length;

            float avgDiff = 0;
            for (int height : top) {
                avgDiff += Math.abs(meanHeight - height);
            }

            return -(avgDiff / top.length) * meanHeightDifferenceWeight;
        }
    }

    // The maximum column height of the state.
    public static class MaxColumnHeight implements MoveEvaluator {
        @Override
        public Float map(MoveResult result) {
            int[] top = result.getState().getTop();

            int maxHeight = Integer.MIN_VALUE;
            for (int column = 0; column < top.length; ++column) {
                int height = top[column];
                if (height > maxHeight) {
                    maxHeight = height;
                }
            }

            return -(float) maxHeight * maxColumnHeightWeight;
        }
    }

    // Number of rows cleared.
    public static class NumRowsCleared implements MoveEvaluator {
        @Override
        public Float map(MoveResult moveResult) {
            return moveResult.getRowsCleared() * numRowsClearedWeight;
        }
    }

    // Whether the move results in a loss or not.
    public static class HasLost implements MoveEvaluator {
        @Override
        public Float map(MoveResult result) {
            return result.hasLost() ? -10.0f : 10.0f;
        }
    }

    // Number of holes, a hole is an empty block with a non-empty block above
    // it.
    public static class NumFaults implements MoveEvaluator {
        @Override
        public Float map(MoveResult result) {
            int[][] field = result.getState().getField();
            int[] top = result.getState().getTop();
            int numFaults = 0;

            for (int x = 0; x < State.COLS; ++x) {
                for (int y = top[x] - 1; y >= 0; --y) {
                    if (field[y][x] == 0) {
                        ++numFaults;
                    }
                }
            }

            return -(float) numFaults * numFaultsWeight;
        }
    }

    // Depth of pits, a pit is a column with adjacent columns higher by at least
    // two blocks and the pit depth
    // is defined as the difference between the height of the pit column and the
    // shortest adjacent column.
    public static class PitDepths implements MoveEvaluator {
        @Override
        public Float map(MoveResult result) {
            int[] top = result.getState().getTop();
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

            return -(float) sumOfPitDepths * pitDepthsWeight;

        }
    }

    // Sum of height difference between all pairs of adjacent columns.
    public static class Roughness implements MoveEvaluator {
        @Override
        public Float map(MoveResult result) {
            int[] top = result.getState().getTop();
            int roughness = 0;
            for (int i = 0; i < top.length - 1; ++i) {
                roughness += Math.abs(top[i] - top[i + 1]);
            }
            return -(float) roughness * roughnessWeight;
        }
    }

    // Not in use
    public static class ColumnDiff implements MoveEvaluator {
        public ColumnDiff(int columnA, int columnB) {
            this.columnA = columnA;
            this.columnB = columnB;
        }

        @Override
        public Float map(MoveResult result) {
            int[] top = result.getState().getTop();
            return -(float) Math.abs(top[columnA] - top[columnB]);
        }

        private final int columnA;
        private final int columnB;
    }

    /**
     * A state that is more useful then the provided one. It is immutable and
     * suitable for parallel processing
     */
    public static class ImmutableState {
        /**
         * Construct a state which is identical to the built-in state
         */
        public ImmutableState(State state) {
            field = copyField(state.getField());
            int[] srcTop = state.getTop();
            top = Arrays.copyOf(srcTop, srcTop.length);
            turn = state.getTurnNumber();
        }

        /**
         * Construct a state with the given field and top
         *
         * @param field
         * @param top
         * @param turn
         */
        public ImmutableState(int[][] field, int[] top, int turn) {
            this.field = field;
            this.top = top;
            this.turn = turn;
        }

        /**
         * Construct an empty state
         */
        public ImmutableState() {
            field = new int[State.ROWS][State.COLS];
            top = new int[State.COLS];
            turn = 0;
        }

        public int[][] getField() {
            return field;
        }

        public int[] getTop() {
            return top;
        }

        public int getTurn() {
            return turn;
        }

        /**
         * Make a move
         *
         * @param piece
         * @param orient
         * @param slot
         * @return result of the move
         */
        public MoveResult move(int piece, int orient, int slot) {
            int[][] field = copyField(this.field);
            int[] top = Arrays.copyOf(this.top, this.top.length);
            int turn = this.turn + 1;

            // height if the first column makes contact
            int height = top[slot] - pBottom[piece][orient][0];
            // for each column beyond the first in the piece
            for (int c = 1; c < pWidth[piece][orient]; c++) {
                height = Math.max(height, top[slot + c] - pBottom[piece][orient][c]);
            }

            // check if game ended
            if (height + pHeight[piece][orient] >= ROWS) {
                return new MoveResult(field, top, turn, true, 0);
            }

            // for each column in the piece - fill in the appropriate blocks
            for (int i = 0; i < pWidth[piece][orient]; i++) {
                // from bottom to top of brick
                for (int h = height + pBottom[piece][orient][i]; h < height + pTop[piece][orient][i]; h++) {
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

            return new MoveResult(field, top, turn, false, rowsCleared);
        }

        private static int[][] copyField(int[][] srcField) {
            int[][] copy = new int[ROWS][COLS];

            for (int i = 0; i < ROWS; ++i) {
                for (int j = 0; j < COLS; ++j) {
                    copy[i][j] = srcField[i][j];
                }
            }

            return copy;
        }

        private final int[][] field;
        private final int[] top;
        private final int turn;

        // static
        public static final int COLS = 10;
        public static final int ROWS = 21;
        public static final int N_PIECES = 7;
        // all legal moves - first index is piece type - then a list of 2-length
        // arrays
        private static int[][][] legalMoves = new int[N_PIECES][][];
        // indices for legalMoves
        private static final int ORIENT = 0;
        private static final int SLOT = 1;
        // possible orientations for a given piece type
        private static final int[] pOrients = {1, 2, 4, 4, 4, 2, 2};
        // the next several arrays define the piece vocabulary in detail
        // width of the pieces [piece ID][orientation]
        private static final int[][] pWidth = { {2}, {1, 4}, {2, 3, 2, 3}, {2, 3, 2, 3}, {2, 3, 2, 3}, {3, 2}, {3, 2}};
        // height of the pieces [piece ID][orientation]
        private static int[][] pHeight = { {2}, {4, 1}, {3, 2, 3, 2}, {3, 2, 3, 2}, {3, 2, 3, 2}, {2, 3}, {2, 3}};
        private static int[][][] pBottom = { {{0, 0}}, { {0}, {0, 0, 0, 0}}, { {0, 0}, {0, 1, 1}, {2, 0}, {0, 0, 0}},
            { {0, 0}, {0, 0, 0}, {0, 2}, {1, 1, 0}}, { {0, 1}, {1, 0, 1}, {1, 0}, {0, 0, 0}}, { {0, 0, 1}, {1, 0}},
            { {1, 0, 0}, {0, 1}}};
        private static int[][][] pTop = { {{2, 2}}, { {4}, {1, 1, 1, 1}}, { {3, 1}, {2, 2, 2}, {3, 3}, {1, 1, 2}},
            { {1, 3}, {2, 1, 1}, {3, 3}, {2, 2, 2}}, { {3, 2}, {2, 2, 2}, {2, 3}, {1, 2, 1}}, { {1, 2, 2}, {3, 2}},
            { {2, 2, 1}, {2, 3}}};

        // initialize legalMoves
        static {
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

    public static class MapReduce {
        public MapReduce(ForkJoinPool forkJoinPool) {
            this.forkJoinPool = forkJoinPool;
        }

        public <Src, Dst> void map(MapFunc<Src, Dst> mapFunc, Iterable<Src> inputs, Collection<Dst> outputs) {
            forkJoinPool.invoke(new MapTask<Src, Dst>(mapFunc, inputs, outputs));
        }

        public <SrcT, IntT, DstT> DstT mapReduce(MapFunc<SrcT, IntT> mapFunc, ReduceFunc<IntT, DstT> reduceFunc,
            Iterable<SrcT> inputs) {

            return forkJoinPool.invoke(new MapReduceTask<SrcT, IntT, DstT>(mapFunc, reduceFunc, inputs));
        }

        private final ForkJoinPool forkJoinPool;
    }

    public static interface MapFunc<SrcT, DstT> {
        public DstT map(SrcT input);
    }

    public static interface ReduceFunc<SrcT, DstT> {
        public DstT reduce(Iterable<SrcT> inputs);
    }

    public static class MapTask<SrcT, DstT> extends ForkJoinTask<Void> {
        public MapTask(MapFunc<SrcT, DstT> mapFunc, Iterable<SrcT> inputs, Collection<DstT> outputs) {
            this.mapFunc = mapFunc;
            this.inputs = inputs;
            this.outputs = outputs;
        }

        @Override
        protected boolean exec() {
            ArrayList<ForkJoinTask<DstT>> applyTasks = new ArrayList<ForkJoinTask<DstT>>();
            for (SrcT input : inputs) {
                applyTasks.add(new ApplyTask(input));
            }
            invokeAll(applyTasks);

            for (ForkJoinTask<DstT> applyTask : applyTasks) {
                outputs.add(applyTask.join());
            }

            return true;
        }

        @Override
        public Void getRawResult() {
            return null;
        }

        @Override
        protected void setRawResult(Void value) {
        }

        private final MapFunc<SrcT, DstT> mapFunc;
        private final Iterable<SrcT> inputs;
        private final Collection<DstT> outputs;
        private static final long serialVersionUID = 1L;

        private class ApplyTask extends ForkJoinTask<DstT> {
            public ApplyTask(SrcT input) {
                this.input = input;
            }

            @Override
            protected boolean exec() {
                setRawResult(mapFunc.map(input));
                return true;
            }

            @Override
            public DstT getRawResult() {
                return output;
            }

            @Override
            protected void setRawResult(DstT value) {
                output = value;
            }

            private final SrcT input;
            private DstT output;

            private static final long serialVersionUID = 1L;
        }
    }

    public static class MapReduceTask<SrcT, IntT, DstT> extends ForkJoinTask<DstT> {
        public MapReduceTask(MapFunc<SrcT, IntT> mapFunc, ReduceFunc<IntT, DstT> reduceFunc, Iterable<SrcT> inputs) {
            this.inputs = inputs;
            this.mapFunc = mapFunc;
            this.reduceFunc = reduceFunc;
        }

        @Override
        protected boolean exec() {
            // Map
            ArrayList<IntT> mapResults = new ArrayList<IntT>();
            MapTask<SrcT, IntT> mapTask = new MapTask<SrcT, IntT>(mapFunc, inputs, mapResults);
            mapTask.invoke();
            // Reduce
            setRawResult(reduceFunc.reduce(mapResults));

            return true;
        }

        @Override
        public DstT getRawResult() {
            return output;
        }

        @Override
        protected void setRawResult(DstT value) {
            output = value;
        }

        private final Iterable<SrcT> inputs;
        private DstT output = null;
        private MapFunc<SrcT, IntT> mapFunc;
        private ReduceFunc<IntT, DstT> reduceFunc;
        private static final long serialVersionUID = 1L;
    }

    /**
     * A common interface for different kind of evaluator
     */
    public interface MoveEvaluator extends MapFunc<MoveResult, Float> {
    }

    private static class Move {
        public Move(ImmutableState state, int index, int piece, int orientation, int position) {
            this.state = state;
            this.index = index;
            this.piece = piece;
            this.orientation = orientation;
            this.position = position;
        }

        public ImmutableState getState() {
            return state;
        }

        public int getIndex() {
            return index;
        }

        public int getPiece() {
            return piece;
        }

        public int getOrientation() {
            return orientation;
        }

        public int getPosition() {
            return position;
        }

        private final ImmutableState state;
        private final int index;
        private final int piece;
        private final int orientation;
        private final int position;
    }

    /**
     * A simple class to hold the evaluation result of a move
     */
    public static class EvaluationResult {
        public EvaluationResult(int move, float score) {
            this.move = move;
            this.score = score;
        }

        public int getMove() {
            return move;
        }

        public float getScore() {
            return score;
        }

        private final int move;
        private final float score;
    }

    /**
     * Result of a move, returned by ImmutableState.move
     */
    public static class MoveResult {
        public MoveResult(int field[][], int top[], int turn, boolean lost, int rowsCleared) {
            this.state = new ImmutableState(field, top, turn);
            this.rowsCleared = rowsCleared;
            this.lost = lost;
        }

        public ImmutableState getState() {
            return state;
        }

        public int getRowsCleared() {
            return rowsCleared;
        }

        public boolean hasLost() {
            return lost;
        }

        private final int rowsCleared;
        private final boolean lost;
        private final ImmutableState state;
    }
}