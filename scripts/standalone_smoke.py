"""Patient must launch and write demo data without the Care Team app installed."""
from pathlib import Path

# Reuse the same accessibility-driven helpers as the full-device journey.
helpers = Path('scripts/emulator_smoke.py').read_text(encoding='utf-8').split('# All functions below')[0]
exec(compile(helpers, 'scripts/emulator_smoke.py', 'exec'))

launch('patient', True)
find('Hello, Maya')
tap('Book an appointment', True)
tap('Continue', True)
tap('Choose appointment time', True)
tap('Review appointment', True)
tap('Confirm sample booking', True)
find("You're booked")
print('PASS: patient launches and books in standalone mode without Care Team installed.')
