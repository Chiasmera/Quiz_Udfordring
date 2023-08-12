# Quiz_Udfordring

Denne app har til formål at udnytte opentdp's API til at hente og vise quizzpørgsmål, som brugeren kan interagere emd.
Projektet løser følgende krav:

- Brugeren skal kunne vælge en kategori ud fra alle tilgængelige kategorier.
- Brugeren skal kunne vælge en sværhedsgrad (easy, medium eller hard).
- Efter opsætningen af førnævnte skal brugeren kunne tage quiz’en, samt se om han/hun svarer korrekt eller forkert inden næste spørgsmål præsenteres.
- Vis antal tilgængelige spørgsmål for en given kategori og/eller for hver sværhedsgrad under en kategori.

I skrivende stund er dette build stable, og opfylder de gældende krav, men har flere ting der kan forbedres:
- Bedre brug af coroutines til at udføre asykront arbejde
- Fejlhåndtering
- Session tokens til at sørge for at brugeren ikke støder på samme spørgsmål hele tiden
- Testing.
- Udseende, ift til tema & layout
