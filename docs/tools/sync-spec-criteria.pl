#!/usr/bin/perl
# Rewrites section 2 of each spec in docs/specifications from its backlog source, so the
# criteria are verbatim again after the story changes. Everything outside section 2 is left
# alone — the analysis in sections 3 to 8 still needs a human pass, and check-specs.pl will
# report any AC this script added that nothing yet covers.
#
# Run from the repository root:  perl docs/tools/sync-spec-criteria.pl
use strict;
use warnings;
use utf8;
binmode STDOUT, ':encoding(UTF-8)';

my $BACKLOG = 'docs/backlog';
my $SPECS   = 'docs/specifications';

sub lines {
    my ($p) = @_;
    open my $fh, '<:encoding(UTF-8)', $p or die "$p: $!";
    my @l = map { my $x = $_; $x =~ s/\r?\n$//; $x } <$fh>;
    close $fh;
    return @l;
}

# ordered list of [id, heading, gherkin] from a backlog story
sub criteria {
    my ($path) = @_;
    my @l = lines($path);
    my @out;
    for (my $i = 0; $i < @l; $i++) {
        next unless $l[$i] =~ /^\*\*([A-Z]{2}-AC\d+)\s+—\s+(.+?)\*\*\s*$/;
        my ($id, $title) = ($1, $2);
        my $j = $i + 1;
        $j++ while $j < @l && $l[$j] !~ /^```gherkin/;
        next unless $j < @l;
        my @body;
        $j++;
        push @body, $l[$j++] while $j < @l && $l[$j] !~ /^```/;
        push @out, [ $id, $title, join("\n", @body) ];
        $i = $j;
    }
    return @out;
}

opendir my $dh, $SPECS or die "$SPECS: $!";
my @specs = sort grep { /^US-\d+\.\d+-.*\.md$/ } readdir $dh;
closedir $dh;
@specs = sort {
    my ($ae,$as) = $a =~ /^US-(\d+)\.(\d+)/; my ($be,$bs) = $b =~ /^US-(\d+)\.(\d+)/;
    $ae <=> $be || $as <=> $bs;
} @specs;

my ($changed, $added_total) = (0, 0);
for my $name (@specs) {
    my $spec = "$SPECS/$name";
    my $back = "$BACKLOG/$name";
    unless (-f $back) { print "skip $name — no backlog source\n"; next; }

    my @crit = criteria($back);
    unless (@crit) { print "skip $name — no criteria found in source\n"; next; }

    my @l = lines($spec);

    # locate section 2 and the heading that follows it
    my ($start, $end);
    for my $i (0 .. $#l) {
        if (!defined $start && $l[$i] =~ /^##\s+2\.\s+Acceptance Criteria/) { $start = $i; next; }
        if (defined $start && !defined $end && $l[$i] =~ /^##\s+3\./) { $end = $i; last; }
    }
    unless (defined $start && defined $end) { print "skip $name — section 2 or 3 not found\n"; next; }

    my %before;
    for my $line (@l[$start .. $end - 1]) {
        $before{$1} = 1 if $line =~ /^\*\*([A-Z]{2}-AC\d+)\s/;
    }

    my @section = (
        '## 2. Acceptance Criteria',
        '',
        'Verbatim from the source. These are the only requirements in this document.',
        '',
    );
    for my $c (@crit) {
        my ($id, $title, $body) = @$c;
        push @section, "**$id — $title**", '```gherkin', split(/\n/, $body), '```', '';
    }

    my @new = (@l[0 .. $start - 1], @section, @l[$end .. $#l]);

    my $old_text = join("\n", @l);
    my $new_text = join("\n", @new);
    if ($old_text eq $new_text) { next; }

    open my $out, '>:encoding(UTF-8)', $spec or die "$spec: $!";
    print $out $new_text, "\n";
    close $out;

    my @added = map { $_->[0] } grep { !$before{ $_->[0] } } @crit;
    $changed++;
    $added_total += scalar @added;
    printf "sync %-42s %d criteria%s\n", $name, scalar @crit,
        @added ? (' — new: ' . join(', ', @added)) : '';
}

printf "\n%d spec(s) rewritten, %d new criterion/criteria carried in\n", $changed, $added_total;
