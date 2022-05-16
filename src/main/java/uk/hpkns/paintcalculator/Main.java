package uk.hpkns.paintcalculator;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.ActionListDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

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

    public static void main(String[] args) throws IOException  {
        if (args.length != 0) {
            if (Objects.equals(args[0], "--no-gui")
                    || Objects.equals(args[0], "--nogui")) {

                showText();
            }
        }
        showGUI();
    }

    /**
     * Show the paint calculator using a terminal based GUI, with Swing support for non-terminal backends.
     * @throws IOException If the terminal couldn't be written to freely.
     */
    private static void showGUI() throws IOException {
        ArrayList<ArrayList<double[]>> obstructionTable = new ArrayList<>();

        Terminal term = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(term);
        screen.startScreen();
        final MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));

        // Main window
        BasicWindow window = new BasicWindow();
        window.setTitle("Paint Calculator");
        Panel panel = new Panel();
        panel.setLayoutManager(new GridLayout(1));

        final Table<String> table = new Table<>("Width", "Height", "# Obstructions");
        table.setSelectAction(() -> {
            if (table.getTableModel().getRowCount() == 0) return;
            List<String> row = table.getTableModel().getRow(table.getSelectedRow());

            BasicWindow dlg = new BasicWindow();
            dlg.setHints(List.of(Window.Hint.MODAL, Window.Hint.CENTERED));
            Panel pnl = new Panel();
            pnl.setLayoutManager(new GridLayout(2));

            pnl.addComponent(new Label(String.format("Wall %d", table.getSelectedRow())));
            Button btnDelete = new Button("Delete Wall");
            btnDelete.addListener(button -> {
                if (new MessageDialogBuilder()
                        .setTitle("Deleting wall!")
                        .setText("Are you sure you want to delete this?")
                        .addButton(MessageDialogButton.Yes)
                        .addButton(MessageDialogButton.Cancel)
                        .build()
                        .showDialog(gui) != MessageDialogButton.Yes)
                    return;

                int selected = table.getSelectedRow();
                table.getTableModel().removeRow(selected);
                obstructionTable.remove(selected);
                dlg.close();
            });
            pnl.addComponent(btnDelete);

            pnl.addComponent(new Label("Width"));
            pnl.addComponent(new Label(row.get(0)));

            pnl.addComponent(new Label("Height"));
            pnl.addComponent(new Label(row.get(1)));

            Table<String> tblObs = new Table<>("Obstruction Type", "Size");
            tblObs.setSelectAction(() -> {
                if (tblObs.getTableModel().getRowCount() == 0) return;

                if (new MessageDialogBuilder()
                        .setTitle("Deleting obstruction!")
                        .setText("Are you sure you want to delete this?")
                        .addButton(MessageDialogButton.Yes)
                        .addButton(MessageDialogButton.Cancel)
                        .build()
                        .showDialog(gui) != MessageDialogButton.Yes)
                    return;

                int selected = tblObs.getSelectedRow();
                obstructionTable.get(table.getSelectedRow()).remove(selected);
                tblObs.getTableModel().removeRow(selected);
            });
            // Populate with current obstructions
            for (double[] obs : obstructionTable.get(table.getSelectedRow())) {
                String typ = "unknown";
                String sze = "";
                if (obs[0] == SHAPE_RECTANGULAR) {
                    typ = "rectangular";
                    sze = String.format("%s × %s", obs[1], obs[2]);
                } else if (obs[0] == SHAPE_CIRCULAR) {
                    typ = "circular";
                    sze = String.format("%s radius", obs[1]);
                }
                tblObs.getTableModel().addRow(typ, sze);
            }

            Button btnAddObs = new Button("Add Obstruction");
            btnAddObs.addListener(button -> new ActionListDialogBuilder()
                    .setTitle("Add Obstruction")
                    .setDescription("Choose shape")
                    .addAction("Rectangular", () -> {
                        String width = new TextInputDialogBuilder()
                                .setTitle("Add Obstruction")
                                .setDescription("Enter obstruction width in metres")
                                .setValidationPattern(Pattern.compile("[\\d.]+"), "You must enter a number!")
                                .build()
                                .showDialog(gui);
                        if (width == null) return;

                        String height = new TextInputDialogBuilder()
                                .setTitle("Add Obstruction")
                                .setDescription("Enter obstruction height in metres")
                                .setValidationPattern(Pattern.compile("[\\d.]+"), "You must enter a number!")
                                .build()
                                .showDialog(gui);
                        if (height == null) return;

                        tblObs.getTableModel().addRow("rectangular", String.format("%s × %s", width, height));
                        double[] data = {SHAPE_RECTANGULAR, Double.parseDouble(width), Double.parseDouble(height)};
                        obstructionTable.get(table.getSelectedRow()).add(data);
                    })
                    .addAction("Circular", () -> {
                        String radius = new TextInputDialogBuilder()
                                .setTitle("Add Obstruction")
                                .setDescription("Enter obstruction radius in metres")
                                .setValidationPattern(Pattern.compile("[\\d.]+"), "You must enter a number!")
                                .build()
                                .showDialog(gui);
                        if (radius == null) return;

                        tblObs.getTableModel().addRow("circular", String.format("%s radius", radius));
                        double[] data = {SHAPE_CIRCULAR, Double.parseDouble(radius)};
                        obstructionTable.get(table.getSelectedRow()).add(data);
                    })
                    .build()
                    .showDialog(gui));
            pnl.addComponent(btnAddObs);

            Button btnOk = new Button("OK");
            btnOk.addListener(button -> {
                // Update # obstructions
                table.getTableModel().setCell(2,
                        table.getSelectedRow(),
                        String.valueOf((long) obstructionTable.get(table.getSelectedRow()).size())
                );
                dlg.close();
            });
            pnl.addComponent(btnOk);

            pnl.addComponent(tblObs, GridLayout.createHorizontallyFilledLayoutData());

            dlg.setComponent(pnl);
            gui.addWindow(dlg);
        });

        Button btnAdd = new Button("Add");
        btnAdd.addListener(button -> {
            String width = new TextInputDialogBuilder()
                    .setTitle("Add Wall")
                    .setDescription("Enter wall width in metres")
                    .setValidationPattern(Pattern.compile("[\\d.]+"), "You must enter a number!")
                    .build()
                    .showDialog(gui);
            if (width == null) return;

            String height = new TextInputDialogBuilder()
                    .setTitle("Add Wall")
                    .setDescription("Enter wall height in metres")
                    .setValidationPattern(Pattern.compile("[\\d.]+"), "You must enter a number!")
                    .build()
                    .showDialog(gui);
            if (height == null) return;

            table.getTableModel().addRow(width, height, "0");
            obstructionTable.add(new ArrayList<>());
        });
        panel.addComponent(btnAdd);
        panel.addComponent(table);

        Button btnCalc = new Button("Calculate...");
        btnCalc.addListener(button -> {
            double totalArea = 0d;

            for (List<String> row : table.getTableModel().getRows()) {
                totalArea += Double.parseDouble(row.get(0)) * Double.parseDouble(row.get(1))
                        - calculateObstructions(obstructionTable.get(table.getSelectedRow()).toArray(new double[0][]), 0);
            }

            double paintQtyPerCoat = totalArea / PAINT_LITRES_PER_SQ_M;

            String coats = new TextInputDialogBuilder()
                    .setTitle("Coats")
                    .setDescription("How many coats of paint?")
                    .setValidationPattern(Pattern.compile("\\d+"), "You must enter a number!")
                    .build()
                    .showDialog(gui);
            if (coats == null) return;

            double paintQty = paintQtyPerCoat * Integer.parseInt(coats);

            String canSize = new TextInputDialogBuilder()
                    .setTitle("Can Size")
                    .setDescription("How big are the paint cans (lites)?")
                    .setValidationPattern(Pattern.compile("[\\d.]+"), "You must enter a number!")
                    .build()
                    .showDialog(gui);
            if (canSize == null) return;

            int cans = (int) Math.ceil(paintQty / Double.parseDouble(canSize));

            new MessageDialogBuilder()
                    .setTitle("Computation finished")
                    .setText(String.format("%.3f litres of paint needed!\nEquating to %d paint can(s).", paintQty, cans))
                    .build()
                    .showDialog(gui);
        });
        panel.addComponent(btnCalc);

        window.setComponent(panel);
        window.setHints(List.of(Window.Hint.CENTERED));

        gui.addWindowAndWait(window);
        System.exit(0);
    }

    /**
     * Show the text questionnaire prompt.
     */
    private static void showText() {
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
