App Modeler Tabler Blueprint Experiment

Copy the entire `tabler` folder to:

    config/ui/layouts/tabler/

Expected files:

    layout.html
    layout.js
    layout.css

Restart App Modeler, open Administration, and select `tabler` as the Layout.

This first experiment intentionally does NOT bundle the full Tabler distribution.
It adapts Tabler's vertical-navigation, card, table, form, and page-layout patterns
to the App Modeler Vue/domain/menu contracts. This isolates the blueprint experiment
from external dependency/loading problems.

If this round-trip works, the next experiment can use the actual Tabler compiled
CSS/JS assets while keeping the same App Modeler layout contract.
