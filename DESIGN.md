# FlowCare design system

Native healthcare task interface (Operate mode). Preserve the master specification's warm alabaster, slate blue, sage, lavender, sand and rose palette; no cyan. Use the supplied palette for brand surfaces, with darker semantic foregrounds in light mode and lighter foregrounds in dark mode for contrast.

Self-host Plus Jakarta Sans for interface text and JetBrains Mono for tokens and identifiers. Material 3 type roles, 24 dp page gutters, 8 dp internal spacing increments, 48 dp minimum controls, 14 dp button corners and 18 dp information surfaces. Quiet outlined navigation actions keep the queue and clinical content prominent. Avoid decorative gradients and motion.

Compact screens scroll vertically; action groups wrap for large text and narrow displays. Support safe areas, IME insets, Android system Back and explicit in-app Back. Keep offline, empty, loading, error, success and disabled states visible. Render synthetic-service limitations accurately.

Design references applied: https://github.com/Leonxlnx/taste-skill and https://github.com/pbakaus/impeccable. Android guidance: https://github.com/android/skills/tree/main/system/edge-to-edge. Website-only hero/layout scripts are not applicable to Compose native screens. The user's specified lavender overrides Taste Skill's generic palette preference.

Native screenshots and physical-device interaction remain required for a complete visual acceptance pass; source inspection alone does not prove rendering quality.
