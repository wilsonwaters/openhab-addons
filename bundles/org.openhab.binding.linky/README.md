# Linky Binding

This binding uses the API provided by Enedis to retrieve your energy consumption data.
You need to create an Enedis account [here](https://espace-client-connexion.enedis.fr/auth/UI/Login?realm=particuliers) if you don't have one already.

Please ensure that you have accepted their conditions, and check that you can see graphs on the website.
Especially, check hourly view/graph. Enedis may ask for permission the first time to start collecting hourly data. 
The binding will not provide these informations unless this step is ok.

## Supported Things

There is one supported thing : the `linky` thing is retrieving the consumption of your home from the [Linky electric meter](https://www.enedis.fr/linky-compteur-communicant).

## Discovery

This binding does not provide discovery service.

## Binding Configuration

The binding has no configuration options, all configuration is done at Thing level.

## Thing Configuration

The thing has the following configuration parameters:

| Parameter       | Description                    |
|-----------------|--------------------------------|
| username        | Your Enedis platform username. |
| password        | Your Enedis platform password. |

## Channels

The information that is retrieved is available as these channels:

| Channel ID        | Item Type     | Description                |
|-------------------|---------------|----------------------------|
| daily#yesterday   | Number:Energy | Yesterday energy usage     |
| weekly#thisWeek   | Number:Energy | Current week energy usage  |
| weekly#lastWeek   | Number:Energy | Last week energy usage     |
| monthly#thisMonth | Number:Energy | Current month energy usage |
| monthly#lastMonth | Number:Energy | Last month energy usage    |
| yearly#thisYear   | Number:Energy | Current year energy usage  |
| yearly#lastYear   | Number:Energy | Last year energy usage     |

## Console Commands

The binding provides one specific command you can use in the console.
Enter the command `smarthome:linky` to get the usage.

```
openhab> smarthome:linky
Usage: smarthome:linky <thingUID> report <start day> <end day> [<separator>] - report daily consumptions between two dates
```

The command `report` reports in the console the daily consumptions between two dates.
If no dates are provided, the last 7 are considered by default.
Start and end day are formatted yyyy-mm-dd.

Here is an example of command you can run: `smarthome:linky linky:linky:local report 2020-11-15 2020-12-15`.

## Full Example

### Thing

```
Thing linky:linky:local "Compteur Linky" [ username="example@domaine.fr", password="******" ]
```

### Items

```
Number:Energy ConsoHier "Conso hier [%.0f %unit%]" <energy> { channel="linky:linky:local:daily#yesterday" }
Number:Energy ConsoSemaineEnCours "Conso semaine en cours [%.0f %unit%]" <energy> { channel="linky:linky:local:weekly#thisWeek" }
Number:Energy ConsoSemaineDerniere "Conso semaine dernière [%.0f %unit%]" <energy> { channel="linky:linky:local:weekly#lastWeek" }
Number:Energy ConsoMoisEnCours "Conso mois en cours [%.0f %unit%]" <energy> { channel="linky:linky:local:monthly#thisMonth" }
Number:Energy ConsoMoisDernier "Conso mois dernier [%.0f %unit%]" <energy> { channel="linky:linky:local:monthly#lastMonth" }
Number:Energy ConsoAnneeEnCours "Conso année en cours [%.0f %unit%]" <energy> { channel="linky:linky:local:yearly#thisYear" }
Number:Energy ConsoAnneeDerniere "Conso année dernière [%.0f %unit%]" <energy> { channel="linky:linky:local:yearly#lastYear" }
```
