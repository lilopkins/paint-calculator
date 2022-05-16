import java.util.Scanner;

/**
 * A calculator to calculate the amount of paint needed to paint walls.
 */
public class Main {
    /**
     * The ANSI code to reset any terminal formatting.
     */
    public static String RESET = "\033[0m";

    /**
     * The ANSI code to format the terminal as bold, red text.
     */
    public static String EMPH = "\033[1;91m";

    /**
     * The amount of paint needed to paint 1 square metre.
     */
    public static final float PAINT_LITRES_PER_SQ_M = 4;

    private static final double SHAPE_RECTANGULAR = 1d;
    private static final double SHAPE_CIRCULAR = 2d;
    private static final double SHAPE_OVAL = 3d;

    public static void main(String[] args) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Disable ANSI colours on Windows - doesn't support them!
            EMPH = RESET = "";
        }

        Scanner scanSysIn = new Scanner(System.in);

        System.out.print("How many walls are there?: ");
        int numWalls = scanSysIn.nextInt();

        double[][][] walls = new double[numWalls][][];
        for (int i = 0; i < numWalls; i++) {
            walls[i] = askWall(scanSysIn, i);
            System.out.println("================");
        }

        // Allow editing
        while (true) {
            System.out.printf("%sSummary%s\n", EMPH, RESET);
            printWalls(walls);

            System.out.printf("Enter number of wall to edit, or %d to continue: ", numWalls + 1);
            int option = scanSysIn.nextInt();
            if (option > numWalls || option <= 0)
                break;

            walls[option - 1] = askWall(scanSysIn, option - 1);
        }

        // How many coats of paint?
        System.out.print("Please enter how many coats of paint you want: ");
        int coats = scanSysIn.nextInt();

        double paintQty = PAINT_LITRES_PER_SQ_M * coats * calculateArea(walls);

        // Paint can size
        System.out.print("Please enter how big your paint cans are (litres): ");
        double canSize = scanSysIn.nextDouble();
        System.out.println("================");

        double numberCansExact = paintQty / canSize;
        int numberCans = (int) Math.ceil(numberCansExact);
        String pluralChar = numberCans == 1 ? "" : "s";

        System.out.printf("You will need %d can%s (%.2f litres)!\n", numberCans, pluralChar, paintQty);
    }

    /**
     * Print the walls in a presentable fashion.
     * @param walls The array of walls.
     */
    private static void printWalls(double[][][] walls) {
        for (int i = 0; i < walls.length; i++) {
            double[][] wall = walls[i];
            System.out.printf("Wall %d, with %d obstructions\n", i + 1, wall.length - 1);
            System.out.printf("    %.2f × %.2f metres\n", wall[0][0], wall[0][1]);
            for (int j = 1; j < wall.length; j++) {
                double[] obs = wall[j];
                String typ;
                if (obs[0] == SHAPE_RECTANGULAR)
                    typ = "rectangular";
                else if (obs[0] == SHAPE_CIRCULAR)
                    typ = "circular";
                else if (obs[0] == SHAPE_OVAL)
                    typ = "oval";
                else
                    throw new IllegalArgumentException("Invalid shape");
                System.out.printf("    Obstruction %d, %s\n", j, typ);

                if (obs[0] == SHAPE_RECTANGULAR)
                    System.out.printf("        %.2f × %.2f m\n", obs[1], obs[2]);
                else if (obs[0] == SHAPE_CIRCULAR)
                    System.out.printf("        %.2f m radius\n", obs[1]);
                else if (obs[0] == SHAPE_OVAL)
                    System.out.printf("        %.2f (semi-major) × %.2f (semi-minor) m\n", obs[1], obs[2]);

                System.out.printf("        %.2f m²\n", calculateObstruction(obs));
            }
            System.out.println();
        }
    }

    /**
     * Calculate the area of the walls, minus obstructions.
     * @param walls The array of walls
     * @return The area.
     */
    private static double calculateArea(double[][][] walls) {
        double total = 0d;
        for (double[][] wall : walls) {
            total += (wall[0][0] * wall[0][1]) - calculateObstructions(wall, 1);
        }
        return total;
    }

    /**
     * Query the user for details to calculate the area of the wall.
     * @param scanSysIn The scanner instance connected to the user's input, probably sysin.
     * @param index The zero-based index of the wall.
     * @return An array containing the wall details.
     */
    private static double[][] askWall(Scanner scanSysIn, int index) {
        System.out.printf("%sWall %d%s\n", EMPH, index + 1, RESET);
        // Enter wall width and height
        System.out.print("Please enter the width of the wall (metres): ");
        double width = scanSysIn.nextDouble();

        System.out.print("Please enter the height of the wall (metres): ");
        double height = scanSysIn.nextDouble();

        double baseArea = width * height;

        // Enter potentially infinite obstructions and width and height

        System.out.println("========");
        System.out.print("How many obstructions are there?: ");
        int numObstructions = scanSysIn.nextInt();

        double[][] obstructions = new double[numObstructions][];

        for (int i = 0; i < numObstructions; i++) {
            double[] obstructionDetails = askObstruction(scanSysIn, index, i);
            obstructions[i] = obstructionDetails;
            System.out.println("========");
        }

        double obstructionArea = calculateObstructions(obstructions, 0);
        if (obstructionArea > baseArea) {
            System.err.println("There is no wall left!");
            System.exit(1);
        }

        double[][] arr = new double[numObstructions + 1][];
        arr[0] = new double[] {width, height};
        // Copy elements from obstructions[n] to arr[n + 1]
        System.arraycopy(obstructions, 0, arr, 1, numObstructions);
        return arr;
    }

    /**
     * Calculate the area taken up by the array of obstructions.
     * @param obstructions The array containing obstructions
     * @param startIndex The index in the array for which obstructions begin.
     * @return The area.
     */
    private static double calculateObstructions(double[][] obstructions, int startIndex) {
        double total = 0d;
        for (int i = startIndex; i < obstructions.length; i++) {
            double[] obs = obstructions[i];
            total += calculateObstruction(obs);
        }
        return total;
    }

    /**
     * Calculate the area taken up by one obstruction
     * @param obs The obstruction
     * @return The area.
     */
    private static double calculateObstruction(double[] obs) {
        if (obs[0] == SHAPE_RECTANGULAR) {
            return obs[1] * obs[2];
        } else if (obs[0] == SHAPE_CIRCULAR) {
            return 2 * Math.PI * obs[1];
        } else if (obs[0] == SHAPE_OVAL) {
            return Math.PI * obs[1] * obs[2];
        } else {
            // This should have been checked earlier
            throw new IllegalArgumentException("Invalid shape");
        }
    }

    /**
     * Query the user for details about an obstruction to painting the wall.
     * @param scanSysIn The scanner instance connected to the user's input, probably sysin.
     * @param wallIndex The zero-based index of the wall this obstruction is located on.
     * @param index The zero-based index of this obstruction on the wall.
     * @return An array of values to use to calculate.
     */
    private static double[] askObstruction(Scanner scanSysIn, int wallIndex, int index) {
        System.out.printf("%sWall %d Obstruction %d%s\n", EMPH, wallIndex + 1, index + 1, RESET);

        System.out.println("What is the shape of the obstruction? 1 - rectangular (or square), 2 - circular, 3 - oval");
        int option = scanSysIn.nextInt();
        return switch (option) {
            case 1: // rectangular
                System.out.print("Please enter the width of the obstruction (metres): ");
                double obWidth = scanSysIn.nextDouble();
                System.out.print("Please enter the height of the obstruction (metres): ");
                double obHeight = scanSysIn.nextDouble();
                yield new double[]{SHAPE_RECTANGULAR, obWidth, obHeight};

            case 2: // circular
                System.out.print("Please enter the radius of the obstruction (metres): ");
                double obRadius = scanSysIn.nextDouble();
                yield new double[]{SHAPE_CIRCULAR, obRadius};

            case 3: // oval
                System.out.print("Please enter the length of the semi-major axis of the obstruction (metres): ");
                double obSemiMajor = scanSysIn.nextDouble();
                System.out.print("Please enter the length of the semi-minor axis of the obstruction (metres): ");
                double obSemiMinor = scanSysIn.nextDouble();
                yield new double[]{SHAPE_OVAL, obSemiMajor, obSemiMinor};

            default:
                System.out.println("Nonexistent shape...");
                yield null;
        };
    }
}
