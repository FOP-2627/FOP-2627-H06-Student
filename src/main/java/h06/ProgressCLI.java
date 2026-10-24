package h06;

import org.tudalgo.algoutils.student.annotation.DoNotTouch;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * ProgressCLI - Interactive console application that accesses the student's implemented classes EXCLUSIVELY via reflection.
 * This file INTENTIONALLY has no compile-time dependency.
 * As soon as classes/methods are added to the same folder and everything is
 * recompiled together, this program automatically detects them and unlocks
 * the appropriate menu options - without needing any changes to this file.
 */
@DoNotTouch
public class ProgressCLI {

    // <editor-fold defaultstate="collapsed" desc="========== [ SYSTEM INTERNAL: DO NOT MODIFY BEYOND THIS POINT ] ==========">

    // ............................................................................
    // .                                                                          .
    // .    /\_/\  USER-LAND ENDS HERE                                            .
    // .   ( o.o ) Below lies the Abyss of Implementation Details.                .
    // .    > ^ <  No changes needed beyond this point.                           .
    // .                                                                          .
    // .                           ≽^•⩊•^≼                                        .
    // ............................................................................

    // Colors
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String GRAY = "\u001B[90m";

    private static final String TASK_0 = "Task 0";
    private static final String TASK_1 = "Task 1";
    private static final String TASK_2 = "Task 2";
    private static final String TASK_3 = "Task 3";

    private static final Scanner scanner = new Scanner(System.in);
    private static Object book = null;


    /**
     * Main entry point of the application. Manages the execution loop,
     * runs all progress checks, displays the current spellbook state,
     * prints the interactive menu, and processes user input choices.
     */
    @DoNotTouch
    public static void run() {
        System.out.println(BOLD + CYAN + "=== Spell Book - ProgressCLI ===" + RESET);
        System.out.println("Detects via reflection which classes already exist and");
        System.out.println("automatically unlocks corresponding features. Only public");
        System.out.println("elements are checked (as required by the assignment).");

        boolean running = true;
        while (running) {
            List<Check> checks = runAllChecks();
            printProgress(checks);

            if (book == null && isOk(checks, "SpellBook")) {
                book = tryCreateSpellBook();
            }
            printBookState();

            printMenu(checks);
            String choice = scanner.nextLine().trim();
            if (choice.equals("0")) {
                running = false;
                continue;
            }
            handleChoice(choice, checks);
        }
        System.out.println(CYAN + "Goodbye!" + RESET);
    }

    // ================= Menu =================

    /**
     * Prints the user command-line menu choices. Disables options dynamically
     * using gray formatting if the corresponding class requirements are not met.
     */
    @DoNotTouch
    private static void printMenu(List<Check> checks) {
        boolean bookOk = book != null;
        boolean anySpell = isOk(checks, "IceLanceSpell") || isOk(checks, "FireballSpell") || isOk(checks, "HealSpell");

        System.out.println();
        printMenuLine("1", "Create & add a new spell (optionally decorate)", bookOk && anySpell);
        printMenuLine("2", "Delete spell at index", bookOk);
        printMenuLine("3", "Execute iceCrown()", bookOk);
        printMenuLine("4", "Execute inspiration()", bookOk);
        printMenuLine("5", "Apply antiMage() to this book", bookOk);
        printMenuLine("6", "Show total mana cost", bookOk);
        printMenuLine("7", "Print entire spell book via printSpellBook()", bookOk);
        System.out.println("0) Exit");
        System.out.print("> ");
    }

    /**
     * Helper method to output a formatted line item in the menu. Shows
     * the action as locked if it has not yet been unlocked by structural checks.
     */
    @DoNotTouch
    private static void printMenuLine(String num, String label, boolean unlocked) {
        if (unlocked) {
            System.out.println(num + ") " + label);
        } else {
            System.out.println(GRAY + num + ") " + label + "  [locked]" + RESET);
        }
    }

    /**
     * Routes the user's numeric selection to the appropriate action function.
     */
    @DoNotTouch
    private static void handleChoice(String choice, List<Check> checks) {
        switch (choice) {
            case "1" -> actionCreateAndAddSpell(checks);
            case "2" -> actionDeleteSpell();
            case "3" -> actionRunNoArg("iceCrown", "iceCrown()");
            case "4" -> actionRunNoArg("inspiration", "inspiration()");
            case "5" -> actionAntiMage();
            case "6" -> actionShowTotalMana();
            case "7" -> actionPrintSpellBook();
            default -> System.out.println(RED + "Invalid input." + RESET);
        }
    }


    // ==================================================================
    // ============================= Actions ============================
    // ==================================================================

    /**
     * Orchestrates the creation of a new spell instance using reflection,
     * prompts the user to optionally wrap it in decorators, and appends it
     * to the SpellBook at a specified index.
     */
    @DoNotTouch
    private static void actionCreateAndAddSpell(List<Check> checks) {
        if (book == null) { printLocked(); return; }
        List<String> spellOptions = new ArrayList<>();
        for (String name : List.of("IceLanceSpell", "FireballSpell", "HealSpell")) {
            if (isOk(checks, name)) spellOptions.add(name);
        }
        if (spellOptions.isEmpty()) {
            System.out.println(RED + "No concrete spell class available yet." + RESET);
            return;
        }

        System.out.println("Choose base spell:");
        for (int i = 0; i < spellOptions.size(); i++) {
            System.out.println((i + 1) + ") " + spellOptions.get(i));
        }
        Integer sel = readMenuChoice(spellOptions.size());
        if (sel == null) return;

        Object spell;
        try {
            spell = construct(spellOptions.get(sel - 1), new Class<?>[0]);
        } catch (Exception e) {
            System.out.println(RED + "Could not create spell: " + rootCause(e) + RESET);
            return;
        }

        List<String> decoratorOptions = new ArrayList<>();
        for (String name : List.of("PowerBoostDecorator", "ManaBoostDecorator", "SilenceDecorator")) {
            if (isOk(checks, name)) decoratorOptions.add(name);
        }
        while (!decoratorOptions.isEmpty()) {
            System.out.println("Wrap with a decorator? (0 = done)");
            for (int i = 0; i < decoratorOptions.size(); i++) {
                System.out.println((i + 1) + ") " + decoratorOptions.get(i));
            }
            System.out.print("> ");
            String in = scanner.nextLine().trim();
            if (in.equals("0") || in.isEmpty()) break;
            int di;
            try {
                di = Integer.parseInt(in);
            } catch (NumberFormatException e) {
                System.out.println(RED + "Invalid." + RESET);
                continue;
            }
            if (di < 1 || di > decoratorOptions.size()) {
                System.out.println(RED + "Invalid." + RESET);
                continue;
            }
            try {
                spell = wrapWithDecorator(decoratorOptions.get(di - 1), spell);
                System.out.println(GREEN + "Wrapped with " + decoratorOptions.get(di - 1) + "." + RESET);
            } catch (Exception e) {
                System.out.println(RED + "Error while wrapping: " + rootCause(e) + RESET);
            }
        }

        System.out.print("At which index should it be inserted? (empty = automatic first free position) > ");
        String idxInput = scanner.nextLine().trim();
        try {
            Class<?> spellCls = tryLoad("Spell");
            if (idxInput.isEmpty()) {
                Method addM = book.getClass().getMethod("addSpell", spellCls);
                Object result = addM.invoke(book, spell);
                System.out.println(Boolean.TRUE.equals(result)
                    ? GREEN + "Spell added." + RESET
                    : RED + "addSpell(Spell) reports: Book full (or unexpected return value)." + RESET);
            } else {
                int idx = Integer.parseInt(idxInput);
                Method addM = book.getClass().getMethod("addSpell", spellCls, int.class);
                addM.invoke(book, spell, idx);
                System.out.println(GREEN + "Spell inserted at index " + idx + "." + RESET);
            }
        } catch (InvocationTargetException e) {
            System.out.println(RED + "addSpell() threw: " + rootCause(e) + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Error: " + rootCause(e) + RESET);
        }
    }

    /**
     * Prompts the user for an array index and triggers the 'deleteSpell' method
     * on the loaded SpellBook instance via reflection.
     */
    @DoNotTouch
    private static void actionDeleteSpell() {
        if (book == null) { printLocked(); return; }
        int idx = readInt("Index of the spell to delete > ");
        try {
            Method delM = book.getClass().getMethod("deleteSpell", int.class);
            delM.invoke(book, idx);
            System.out.println(YELLOW + "Slot " + idx + " cleared (deleteSpell(int))." + RESET);
        } catch (InvocationTargetException e) {
            System.out.println(RED + "deleteSpell(int) threw: " + rootCause(e) + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Error: " + rootCause(e) + RESET);
        }
    }


    /**
     * Executes a parameterless method on the current SpellBook instance
     * based on the method's string identifier.
     */
    @DoNotTouch
    private static void actionRunNoArg(String methodName, String humanLabel) {
        if (book == null) { printLocked(); return; }
        try {
            Method m = book.getClass().getMethod(methodName);
            m.invoke(book);
            System.out.println(CYAN + humanLabel + " executed." + RESET);
        } catch (NoSuchMethodException e) {
            System.out.println(RED + methodName + "() is not implemented yet." + RESET);
        } catch (InvocationTargetException e) {
            System.out.println(RED + methodName + "() threw: " + rootCause(e) + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Error: " + rootCause(e) + RESET);
        }
    }

    /**
     * Executes the static 'antiMage' validation method on the SpellBook class,
     * verifying that it is correctly declared static and modifies the target spellbook.
     */
    @DoNotTouch
    private static void actionAntiMage() {
        if (book == null) { printLocked(); return; }
        try {
            Class<?> bookCls = book.getClass();
            Method m = bookCls.getMethod("antiMage", bookCls);
            if (!Modifier.isStatic(m.getModifiers())) {
                System.out.println(RED + "antiMage(SpellBook) should be static." + RESET);
                return;
            }
            m.invoke(null, book);
            System.out.println(CYAN + "antiMage() executed: all spells in this book are now silenced." + RESET);
        } catch (NoSuchMethodException e) {
            System.out.println(RED + "antiMage(SpellBook) is not implemented yet." + RESET);
        } catch (InvocationTargetException e) {
            System.out.println(RED + "antiMage() threw: " + rootCause(e) + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Error: " + rootCause(e) + RESET);
        }
    }

    /**
     * Invokes the total mana cost aggregation logic on the book via reflection
     * and shows the aggregated output.
     */
    @DoNotTouch
    private static void actionShowTotalMana() {
        if (book == null) { printLocked(); return; }
        try {
            Object result = book.getClass().getMethod("calculateTotalManaCost").invoke(book);
            System.out.println("Total mana cost: " + YELLOW + result + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Error: " + rootCause(e) + RESET);
        }
    }

    /**
     * Directly calls the printSpellBook method via reflection to execute the
     * native printing logic inside the student's solution.
     */
    @DoNotTouch
    private static void actionPrintSpellBook() {
        if (book == null) { printLocked(); return; }
        try {
            System.out.println(GRAY + "--- printSpellBook() Output ---" + RESET);
            book.getClass().getMethod("printSpellBook").invoke(book);
        } catch (Exception e) {
            System.out.println(RED + "Error: " + rootCause(e) + RESET);
        }
    }

    /**
     * Wraps a concrete target spell into one of the available decorator types by
     * requesting additional runtime user metrics (like boost intensity or status toggle).
     */
    @DoNotTouch
    private static Object wrapWithDecorator(String decClass, Object wrapped) throws ReflectiveOperationException {
        Class<?> spellCls = tryLoad("Spell");
        return switch (decClass) {
            case "PowerBoostDecorator" -> construct(decClass, new Class<?>[]{spellCls, int.class},
                wrapped, readInt("Bonus value > "));
            case "ManaBoostDecorator" -> construct(decClass, new Class<?>[]{spellCls, int.class},
                wrapped, readInt("New mana cost > "));
            case "SilenceDecorator" -> {
                System.out.print("Silence? (y/n) > ");
                boolean silence = scanner.nextLine().trim().equalsIgnoreCase("y");
                yield construct(decClass, new Class<?>[]{spellCls, boolean.class}, wrapped, silence);
            }
            default -> throw new IllegalStateException("Unknown decorator: " + decClass);
        };
    }

    /**
     * Prints a standardized warning message.
     */
    @DoNotTouch
    private static void printLocked() {
        System.out.println(RED + "Locked: requires " + "SpellBook" + " (not present yet or faulty)." + RESET);
    }

    // ================= SpellBook Display =================

    /**
     * Tries to instantiate a new SpellBook instance via reflection, returning
     * null if the class hasn't been implemented yet.
     */
    @DoNotTouch
    private static Object tryCreateSpellBook() {
        try {
            return construct("SpellBook", new Class<?>[0]);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Inspects and outputs the visual dashboard representing the inner layout
     * of the SpellBook. If enhanced lookup capabilities are ready, it switches to a full matrix list.
     */
    @DoNotTouch
    private static void printBookState() {
        if (book == null) return;
        Class<?> bookCls = book.getClass();
        System.out.println();
        System.out.println(BOLD + "--- Current Spell Book ---" + RESET);

        try {
            Object total = bookCls.getMethod("calculateTotalManaCost").invoke(book);
            System.out.println("Total mana cost: " + YELLOW + total + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Error displaying: " + rootCause(e) + RESET);
        }
    }


    /**
     * Value object structure representing a diagnostic compilation check result.
     */
    private record Check(String label, String task, boolean ok, String detail) {}

    /**
     * Sequentially assembles and executes the compilation structural diagnostics
     * checklist for tracking current milestone completion status.
     */
    @DoNotTouch
    private static List<Check> runAllChecks() {
        List<Check> checks = new ArrayList<>();
        checks.add(checkGameConstants());
        checks.add(checkTargetType());
        checks.add(checkSpell());
        checks.add(checkOffensiveSpell());
        checks.add(checkConcreteOffensiveSpell("IceLanceSpell", 3, "IceLance", 15, 0, "SINGLE_TARGET"));
        checks.add(checkConcreteOffensiveSpell("FireballSpell", 4, "FireBall", 25, 5, "AREA_OF_EFFECT"));
        checks.add(checkSupportSpell());
        checks.add(checkHealSpell());
        checks.add(checkSpellDecorator());
        checks.add(checkPowerBoostDecorator());
        checks.add(checkManaBoostDecorator());
        checks.add(checkSilenceDecorator());
        checks.add(checkSpellBook());
        return checks;
    }


    /**
     * Iterates through all test checklist findings and outputs an interactive
     * visualization detailing completed vs missing structures.
     */
    @DoNotTouch
    private static void printProgress(List<Check> checks) {
        System.out.println();
        System.out.println(BOLD + "--- Progress ---" + RESET);
        String currentTask = null;
        for (Check c : checks) {
            if (!c.task().equals(currentTask)) {
                currentTask = c.task();
                System.out.println(GRAY + currentTask + RESET);
            }
            String mark = c.ok() ? (GREEN + "[x]" + RESET) : (RED + "[ ]" + RESET);
            String detail = c.detail() != null ? (GRAY + "  " + c.detail() + RESET) : "";
            System.out.println("  " + mark + " " + c.label() + detail);
        }
        long done = checks.stream().filter(Check::ok).count();
        System.out.println(BOLD + done + "/" + checks.size() + " components complete.");
    }

    // ================= Checks =================

    /**
     * Inspects the structure and static initialization parameters of the GameConstants class.
     */
    @DoNotTouch
    private static Check checkGameConstants() {
        Class<?> cls = tryLoad("GameConstants");
        if (cls == null) return fail("GameConstants", TASK_0, "Class is missing");
        try {
            int ice = cls.getField("ICE_SPEAR_DAMAGE").getInt(null);
            int fire = cls.getField("FIRE_BALL_DAMAGE").getInt(null);
            int burn = cls.getField("BURN_DAMAGE").getInt(null);
            int maxSize = cls.getField("MAX_SPELLBOOK_SIZE").getInt(null);
            List<String> errors = new ArrayList<>();
            expectInt(errors, "ICE_SPEAR_DAMAGE", ice, 15);
            expectInt(errors, "FIRE_BALL_DAMAGE", fire, 25);
            expectInt(errors, "BURN_DAMAGE", burn, 5);
            expectInt(errors, "MAX_SPELLBOOK_SIZE", maxSize, 42);
            return errors.isEmpty() ? pass("GameConstants", TASK_0) : fail("GameConstants", TASK_0, join(errors));
        } catch (NullPointerException e) {
            return fail("GameConstants", TASK_0, "Constants must be 'static'");
        } catch (NoSuchFieldException e) {
            return fail("GameConstants", TASK_0, "Constant is missing or not public: " + e.getMessage());
        } catch (Exception e) {
            return fail("GameConstants", TASK_0, "Error: " + e);
        }
    }

    /**
     * Structural reflection verification for the TargetType enum structure.
     */
    @DoNotTouch
    private static Check checkTargetType() {
        Class<?> cls = tryLoad("TargetType");
        if (cls == null) return fail("TargetType", TASK_0, "Enum is missing");
        if (!cls.isEnum()) return fail("TargetType", TASK_0, "Is not an enum");
        var names = Arrays.stream(cls.getEnumConstants()).map(Object::toString).collect(Collectors.toSet());
        List<String> expected = List.of("SINGLE_TARGET", "AREA_OF_EFFECT", "SELF");
        List<String> missing = expected.stream().filter(n -> !names.contains(n)).toList();
        if (!missing.isEmpty()) return fail("TargetType", TASK_0, "Missing values: " + missing);
        return pass("TargetType", TASK_0);
    }

    @DoNotTouch
    private static Check checkSpell() {
        Class<?> cls = tryLoad("Spell");
        if (cls == null) return fail("Spell", TASK_1, "Class is missing");
        if (!Modifier.isAbstract(cls.getModifiers())) return fail("Spell", TASK_1, "Should be abstract");
        List<String> missing = missingMethods(cls, "getSpellName", "getTargetType", "getManaCost", "isActive", "getEffectStrength");
        if (!missing.isEmpty()) return fail("Spell", TASK_1, "Missing methods: " + missing);
        if (hasConstructor(cls, int.class, String.class)) {
            return fail("Spell", TASK_1, "Constructor (int manaCost, String spellName) is missing");
        }
        return pass("Spell", TASK_1);
    }

    @DoNotTouch
    private static Check checkOffensiveSpell() {
        Class<?> cls = tryLoad("OffensiveSpell");
        Class<?> spellCls = tryLoad("Spell");
        if (cls == null) return fail("OffensiveSpell", TASK_1, "Class is missing");
        if (spellCls == null) return fail("OffensiveSpell", TASK_1, "Requires Spell (missing)");
        if (!Modifier.isAbstract(cls.getModifiers())) return fail("OffensiveSpell", TASK_1, "Should be abstract");
        if (!spellCls.equals(cls.getSuperclass())) return fail("OffensiveSpell", TASK_1, "Does not directly extend Spell");
        List<String> missing = missingMethods(cls, "getBaseDamage", "getSecondaryDamage");
        if (!missing.isEmpty()) return fail("OffensiveSpell", TASK_1, "Missing methods: " + missing);
        if (hasConstructor(cls, int.class, String.class)) {
            return fail("OffensiveSpell", TASK_1, "Constructor (int, String) is missing");
        }
        return pass("OffensiveSpell", TASK_1);
    }

    @DoNotTouch
    private static Check checkConcreteOffensiveSpell(String className, int expectedManaCost, String expectedName,
                                                     int expectedBaseDamage, int expectedSecondaryDamage, String expectedTargetType) {
        Class<?> cls = tryLoad(className);
        Class<?> offCls = tryLoad("OffensiveSpell");
        if (cls == null) return fail(className, TASK_1, "Class is missing");
        if (offCls == null) return fail(className, TASK_1, "Requires OffensiveSpell (missing)");
        if (!offCls.isAssignableFrom(cls)) return fail(className, TASK_1, "Does not extend OffensiveSpell");
        try {
            Object instance = construct(className, new Class<?>[0]);
            List<String> errors = new ArrayList<>();
            expectInt(errors, "manaCost", (int) invokeNoArg(instance, "getManaCost"), expectedManaCost);
            expectString(errors, (String) invokeNoArg(instance, "getSpellName"), expectedName);
            expectInt(errors, "baseDamage", (int) invokeNoArg(instance, "getBaseDamage"), expectedBaseDamage);
            expectInt(errors, "secondaryDamage", (int) invokeNoArg(instance, "getSecondaryDamage"), expectedSecondaryDamage);
            expectToString(errors, invokeNoArg(instance, "getTargetType"), expectedTargetType);
            return errors.isEmpty() ? pass(className, TASK_1) : fail(className, TASK_1, join(errors));
        } catch (NoSuchMethodException e) {
            return fail(className, TASK_1, "No-arg constructor is missing");
        } catch (InvocationTargetException e) {
            return fail(className, TASK_1, "Throws " + rootCause(e));
        } catch (Exception e) {
            return fail(className, TASK_1, "Error: " + e);
        }
    }

    @DoNotTouch
    private static Check checkSupportSpell() {
        Class<?> cls = tryLoad("SupportSpell");
        Class<?> spellCls = tryLoad("Spell");
        if (cls == null) return fail("SupportSpell", TASK_1, "Class is missing");
        if (spellCls == null) return fail("SupportSpell", TASK_1, "Requires Spell (missing)");
        if (!Modifier.isAbstract(cls.getModifiers())) return fail("SupportSpell", TASK_1, "Should be abstract");
        if (!spellCls.equals(cls.getSuperclass())) return fail("SupportSpell", TASK_1, "Does not directly extend Spell");
        if (!hasMethod(cls, "getEffectDuration")) return fail("SupportSpell", TASK_1, "getEffectDuration() is missing");
        if (hasConstructor(cls, int.class, String.class)) {
            return fail("SupportSpell", TASK_1, "Constructor (int, String) is missing");
        }
        return pass("SupportSpell", TASK_1);
    }

    @DoNotTouch
    private static Check checkHealSpell() {
        Class<?> cls = tryLoad("HealSpell");
        Class<?> supCls = tryLoad("SupportSpell");
        if (cls == null) return fail("HealSpell", TASK_1, "Class is missing");
        if (supCls == null) return fail("HealSpell", TASK_1, "Requires SupportSpell (missing)");
        if (!supCls.isAssignableFrom(cls)) return fail("HealSpell", TASK_1, "Does not extend SupportSpell");
        try {
            Object instance = construct("HealSpell", new Class<?>[0]);
            List<String> errors = new ArrayList<>();
            expectInt(errors, "manaCost", (int) invokeNoArg(instance, "getManaCost"), 2);
            expectString(errors, (String) invokeNoArg(instance, "getSpellName"), "HealSpell");
            expectInt(errors, "effectStrength", (int) invokeNoArg(instance, "getEffectStrength"), 2);
            expectInt(errors, "effectDuration", (int) invokeNoArg(instance, "getEffectDuration"), 4);
            expectToString(errors, invokeNoArg(instance, "getTargetType"), "SELF");
            return errors.isEmpty() ? pass("HealSpell", TASK_1) : fail("HealSpell", TASK_1, join(errors));
        } catch (NoSuchMethodException e) {
            return fail("HealSpell", TASK_1, "No-arg constructor is missing");
        } catch (InvocationTargetException e) {
            return fail("HealSpell", TASK_1, "Throws " + rootCause(e));
        } catch (Exception e) {
            return fail("HealSpell", TASK_1, "Error: " + e);
        }
    }

    @DoNotTouch
    private static Check checkSpellDecorator() {
        Class<?> cls = tryLoad("SpellDecorator");
        Class<?> spellCls = tryLoad("Spell");
        if (cls == null) return fail("SpellDecorator", TASK_2, "Class is missing");
        if (spellCls == null) return fail("SpellDecorator", TASK_2, "Requires Spell (missing)");
        if (!Modifier.isAbstract(cls.getModifiers())) return fail("SpellDecorator", TASK_2, "Should be abstract");
        if (!spellCls.equals(cls.getSuperclass())) return fail("SpellDecorator", TASK_2, "Does not directly extend Spell");
        if (hasConstructor(cls, spellCls)) return fail("SpellDecorator", TASK_2, "Constructor (Spell) is missing");
        List<String> missing = missingMethods(cls, "getSpellName", "getTargetType", "getManaCost", "isActive", "getEffectStrength");
        if (!missing.isEmpty()) return fail("SpellDecorator", TASK_2, "Missing methods: " + missing);
        return pass("SpellDecorator", TASK_2);
    }

    @DoNotTouch
    private static Check checkPowerBoostDecorator() {
        String name = "PowerBoostDecorator";
        Class<?> cls = tryLoad(name);
        Class<?> decCls = tryLoad("SpellDecorator");
        Class<?> spellCls = tryLoad("Spell");
        String structuralError = decoratorStructuralError(cls, decCls, spellCls);
        if (structuralError != null) return fail(name, TASK_2, structuralError);

        Constructor<?> ctor;
        try {
            ctor = cls.getDeclaredConstructor(spellCls, int.class);
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            return fail(name, TASK_2, "Constructor (Spell, int) is missing");
        }

        Optional<Object> base = getAnyConcreteSpellInstance();
        if (base.isEmpty()) return passUntested(name);

        try {
            int before = (int) invokeNoArg(base.get(), "getEffectStrength");
            Object wrapped = ctor.newInstance(base.get(), 10);
            int after = (int) invokeNoArg(wrapped, "getEffectStrength");
            if (after != before + 10) {
                return fail(name, TASK_2, "getEffectStrength() returns " + after + " (expected " + (before + 10) + ")");
            }
            return pass(name, TASK_2);
        } catch (InvocationTargetException e) {
            return fail(name, TASK_2, "Throws " + rootCause(e));
        } catch (Exception e) {
            return fail(name, TASK_2, "Error: " + e);
        }
    }

    @DoNotTouch
    private static Check checkManaBoostDecorator() {
        String name = "ManaBoostDecorator";
        Class<?> cls = tryLoad(name);
        Class<?> decCls = tryLoad("SpellDecorator");
        Class<?> spellCls = tryLoad("Spell");
        String structuralError = decoratorStructuralError(cls, decCls, spellCls);
        if (structuralError != null) return fail(name, TASK_2, structuralError);

        Constructor<?> ctor;
        try {
            ctor = cls.getDeclaredConstructor(spellCls, int.class);
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            return fail(name, TASK_2, "Constructor (Spell, int) is missing");
        }

        Optional<Object> base = getAnyConcreteSpellInstance();
        if (base.isEmpty()) return passUntested(name);

        try {
            Object wrapped = ctor.newInstance(base.get(), 99);
            int mana = (int) invokeNoArg(wrapped, "getManaCost");
            if (mana != 99) return fail(name, TASK_2, "getManaCost() returns " + mana + " (expected 99)");
            return pass(name, TASK_2);
        } catch (InvocationTargetException e) {
            return fail(name, TASK_2, "Throws " + rootCause(e));
        } catch (Exception e) {
            return fail(name, TASK_2, "Error: " + e);
        }
    }

    @DoNotTouch
    private static Check checkSilenceDecorator() {
        String name = "SilenceDecorator";
        Class<?> cls = tryLoad(name);
        Class<?> decCls = tryLoad("SpellDecorator");
        Class<?> spellCls = tryLoad("Spell");
        String structuralError = decoratorStructuralError(cls, decCls, spellCls);
        if (structuralError != null) return fail(name, TASK_2, structuralError);

        Constructor<?> ctor;
        try {
            ctor = cls.getDeclaredConstructor(spellCls, boolean.class);
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            return fail(name, TASK_2, "Constructor (Spell, boolean) is missing");
        }

        Optional<Object> base = getAnyConcreteSpellInstance();
        if (base.isEmpty()) return passUntested(name);

        try {
            Object silenced = ctor.newInstance(base.get(), true);
            boolean activeWhenSilenced = (boolean) invokeNoArg(silenced, "isActive");
            Object notSilenced = ctor.newInstance(base.get(), false);
            boolean activeWhenNotSilenced = (boolean) invokeNoArg(notSilenced, "isActive");
            List<String> errors = new ArrayList<>();
            if (activeWhenSilenced) errors.add("isActive()=true when silenceState=true (expected false)");
            if (!activeWhenNotSilenced) errors.add("isActive()=false when silenceState=false, even though the base spell is active");
            return errors.isEmpty() ? pass(name, TASK_2) : fail(name, TASK_2, join(errors));
        } catch (InvocationTargetException e) {
            return fail(name, TASK_2, "Throws " + rootCause(e));
        } catch (Exception e) {
            return fail(name, TASK_2, "Error: " + e);
        }
    }

    @DoNotTouch
    private static Check checkSpellBook() {
        Class<?> cls = tryLoad("SpellBook");
        Class<?> spellCls = tryLoad("Spell");
        if (cls == null) return fail("SpellBook", TASK_3, "Class is missing");
        if (spellCls == null) return fail("SpellBook", TASK_3, "Requires Spell (missing)");

        List<String> missing = new ArrayList<>();
        if (hasConstructor(cls)) missing.add("Constructor ()");
        if (!hasMethod(cls, "addSpell", spellCls)) missing.add("addSpell(Spell)");
        if (!hasMethod(cls, "addSpell", spellCls, int.class)) missing.add("addSpell(Spell, int)");

        if (!hasMethod(cls, "deleteSpell", int.class)) missing.add("deleteSpell(int)");
        if (!hasMethod(cls, "calculateTotalManaCost")) missing.add("calculateTotalManaCost()");
        if (!hasMethod(cls, "printSpellBook")) missing.add("printSpellBook()");
        if (!hasMethod(cls, "iceCrown")) missing.add("iceCrown()");
        if (!hasMethod(cls, "inspiration")) missing.add("inspiration()");
        if (!hasStaticMethod(cls, cls)) missing.add("antiMage(SpellBook) [static]");
        if (!missing.isEmpty()) return fail("SpellBook", TASK_3, "Missing: " + join(missing));



        return pass("SpellBook", TASK_3);
    }

    // ================= Check helpers =================

    /**
     * Builds a passing Check with no detail message.
     */
    private static Check pass(String label, String task) {
        return new Check(label, task, true, null);
    }

    /**
     * Builds a passing Check whose behavior could not be verified because no
     * concrete spell instance was available yet to test against.
     */
    private static Check passUntested(String label) {
        return new Check(label, ProgressCLI.TASK_2, true, "(Structure OK, behavior not tested)");
    }

    /**
     * Builds a failing Check with the given detail message.
     */
    private static Check fail(String label, String task, String detail) {
        return new Check(label, task, false, detail);
    }

    /**
     * Joins a list of error fragments the same way across all checks.
     */
    private static String join(List<String> parts) {
        return String.join(", ", parts);
    }

    /**
     * Compares an actual int against the expected value, appending a formatted
     * mismatch message to errors if they differ.
     */
    private static void expectInt(List<String> errors, String field, int actual, int expected) {
        if (actual != expected) errors.add(field + "=" + actual + " (expected " + expected + ")");
    }

    /**
     * Compares an actual String against the expected value, appending a formatted
     * (quoted) mismatch message to errors if they differ.
     */
    private static void expectString(List<String> errors, String actual, String expected) {
        if (!expected.equals(actual)) errors.add("spellName" + "=\"" + actual + "\" (expected \"" + expected + "\")");
    }

    /**
     * Compares the toString() of an actual (possibly enum) value against the
     * expected value, appending a formatted mismatch message if they differ or if actual is null.
     */
    private static void expectToString(List<String> errors, Object actual, String expected) {
        if (actual == null || !expected.equals(actual.toString())) {
            errors.add("targetType" + "=" + actual + " (expected " + expected + ")");
        }
    }

    /**
     * Common structural prerequisite for all SpellDecorator subclasses: the class
     * must exist, SpellDecorator/Spell must be loadable, and it must extend SpellDecorator.
     * Returns an error message, or null if the structure is fine.
     */
    private static String decoratorStructuralError(Class<?> cls, Class<?> decCls, Class<?> spellCls) {
        if (cls == null) return "Class is missing";
        if (decCls == null || spellCls == null) return "Requires SpellDecorator (missing)";
        if (!decCls.isAssignableFrom(cls)) return "Does not extend SpellDecorator";
        return null;
    }

    /**
     * Returns the subset of the given no-arg method names that the class does NOT expose publicly.
     */
    private static List<String> missingMethods(Class<?> cls, String... methodNames) {
        List<String> missing = new ArrayList<>();
        for (String name : methodNames) {
            if (!hasMethod(cls, name)) missing.add(name);
        }
        return missing;
    }

    /**
     * Verifies if a class exposes a declared constructor with the given parameter types.
     */
    private static boolean hasConstructor(Class<?> cls, Class<?>... paramTypes) {
        try {
            cls.getDeclaredConstructor(paramTypes);
            return false;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }


    /**
     * Attempts to dynamically fetch a class type by string identifier across
     * root or module sub-packages. Returns null if not found.
     */
    @DoNotTouch
    private static Class<?> tryLoad(String name) {
        String[] possiblePackages = {
            "h06.",
            "h06.spells.",
            "h06.decorators.",
            "h06.constants.",
            "" // root
        };

        for (String pkg : possiblePackages) {
            try {
                return Class.forName(pkg + name);
            } catch (ClassNotFoundException ignored) {
                // see next folder/ package
            }
        }
        return null;
    }

    /**
     * Verifies if a given class structure explicitly exposes a specific named method type signature.
     */
    @DoNotTouch
    private static boolean hasMethod(Class<?> cls, String name, Class<?>... paramTypes) {
        try {
            cls.getMethod(name, paramTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @DoNotTouch
    private static boolean hasStaticMethod(Class<?> cls, Class<?>... paramTypes) {
        try {
            Method m = cls.getMethod("antiMage", paramTypes);
            return Modifier.isStatic(m.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Helper to dynamically instantiate a target class type using reflective argument mapping.
     */
    @DoNotTouch
    private static Object construct(String className, Class<?>[] paramTypes, Object... args) throws ReflectiveOperationException {
        Class<?> cls = tryLoad(className);
        if (cls == null) {
            throw new ClassNotFoundException(className);
        }
        return cls.getConstructor(paramTypes).newInstance(args);
    }

    /**
     * Invokes a parameterless method on a given object instance using reflection.
     */
    @DoNotTouch
    private static Object invokeNoArg(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }

    /**
     * Probes all completed target classes to extract any single valid instantiated baseline spell.
     */
    @DoNotTouch
    private static Optional<Object> getAnyConcreteSpellInstance() {
        for (String name : List.of("IceLanceSpell", "FireballSpell", "HealSpell")) {
            if (tryLoad(name) != null) {
                try {
                    return Optional.of(construct(name, new Class<?>[0]));
                } catch (Exception ignored) {
                    // try next class
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Evaluation utility tracking structural validation task records.
     */
    @DoNotTouch
    private static boolean isOk(List<Check> checks, String label) {
        return checks.stream().anyMatch(c -> c.label().equals(label) && c.ok());
    }

    /**
     * Unwraps deep exception trace nesting hierarchies to expose the primary failure root cause.
     */
    @DoNotTouch
    private static String rootCause(Exception e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
    }




    /**
     * Displays a dynamic command selection window, loops input validation prompts,
     * and filters acceptable numeric ranges.
     */
    @DoNotTouch
    private static Integer readMenuChoice(int max) {
        System.out.print("> ");
        try {
            int v = Integer.parseInt(scanner.nextLine().trim());
            if (v < 1 || v > max) {
                System.out.println(RED + "Invalid choice." + RESET);
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid input." + RESET);
            return null;
        }
    }

    /**
     * Prints an interactive string instruction and processes numeric primitive returns safely.
     */
    @DoNotTouch
    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(RED + "Please enter a number." + RESET);
            }
        }
    }
    // </editor-fold>
}
