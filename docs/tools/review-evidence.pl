#!/usr/bin/perl
# Reviewer-side evidence gathering for specs that use per-story AC prefixes
# (LI-AC1, TQ-AC3, ...). The trace_check scripts bundled with story-spec-reviewer
# match only the bare `AC-<n>` form, so against this backlog they find zero
# criteria and then report "OK" over an empty set. This collects the same signals
# for the real ID scheme, plus the untagged-normative scan.
#
#   perl docs/tools/review-evidence.pl [spec-file ...]
use strict;
use warnings;
use utf8;
binmode STDOUT, ':encoding(UTF-8)';

my $SPECS = 'docs/specifications';
my @files = @ARGV;
unless (@files) {
    opendir my $d, $SPECS or die "$SPECS: $!";
    @files = map { "$SPECS/$_" } sort grep { /^US-\d+\.\d+-.*\.md$/ } readdir $d;
    closedir $d;
    @files = sort {
        my ($ae,$as) = $a =~ /US-(\d+)\.(\d+)/; my ($be,$bs) = $b =~ /US-(\d+)\.(\d+)/;
        $ae <=> $be || $as <=> $bs;
    } @files;
}

# sections that state requirements; a tag elsewhere is not coverage
my $NORMATIVE = qr/^##\s+(3\.|4\.)/;
my $OTHER_H2  = qr/^##\s+/;

my $total_untagged = 0;
my $total_specs    = 0;

for my $path (@files) {
    open my $fh, '<:encoding(UTF-8)', $path or die "$path: $!";
    my @l = map { my $x = $_; $x =~ s/\r?\n$//; $x } <$fh>;
    close $fh;
    $total_specs++;

    my (%declared, @order);
    my $in_ac = 0;
    for my $line (@l) {
        $in_ac = ($line =~ /^##\s+2\.\s+Acceptance Criteria/) ? 1
               : ($line =~ $OTHER_H2 ? 0 : $in_ac);
        next unless $in_ac;
        if ($line =~ /^\*\*([A-Z]{2}-AC\d+)\s/) { $declared{$1} = 1; push @order, $1 }
    }

    # walk normative sections, collecting paragraphs
    my (@untagged, %tagged_in_body);
    my ($in_norm, @para) = (0);
    my $flush = sub {
        return unless @para;
        my $text = join(' ', @para);
        @para = ();
        return if $text =~ /^\s*$/;
        return if $text =~ /^\|/;              # table row
        return if $text =~ /^#/;
        my @tags = $text =~ /\[([A-Z]{2}-AC\d+)\]/g;
        $tagged_in_body{$_} = 1 for @tags;
        return if @tags;
        # normative-sounding but untagged
        return unless $text =~ /\b(respond|responds|is rejected|must|MUST|the response is|is written|is set|is returned|is created|is revoked|is queued|is sent|is stored)\b/;
        my $short = length($text) > 150 ? substr($text, 0, 150) . '…' : $text;
        push @untagged, $short;
    };
    for my $line (@l) {
        if ($line =~ $OTHER_H2) { $flush->(); $in_norm = ($line =~ $NORMATIVE) ? 1 : 0; next }
        next unless $in_norm;
        if ($line =~ /^\s*$/) { $flush->(); next }
        push @para, $line;
    }
    $flush->();

    # matrix rows
    my (%rows, $in_matrix);
    for my $line (@l) {
        $in_matrix = ($line =~ /^##\s+8\./) ? 1 : ($line =~ $OTHER_H2 ? 0 : $in_matrix);
        next unless $in_matrix;
        $rows{$1}++ if $line =~ /^\|\s*([A-Z]{2}-AC\d+)\s*\|/;
    }

    my @uncovered = grep { !$tagged_in_body{$_} } @order;
    my @nomatrix  = grep { ($rows{$_} // 0) != 1 } @order;
    my %all_tags;
    my $whole = join("\n", @l);
    $all_tags{$1} = 1 while $whole =~ /\[([A-Z]{2}-AC\d+)\]/g;
    my ($own_prefix) = ($order[0] // '') =~ /^([A-Z]{2})-/;
    my @dangling = grep { defined $own_prefix && /^\Q$own_prefix\E-/ && !$declared{$_} } sort keys %all_tags;

    $total_untagged += scalar @untagged;

    my $name = $path; $name =~ s{.*/}{};
    printf "%-42s declared %2d", $name, scalar @order;
    print  @uncovered ? "  UNCOVERED: @uncovered" : "";
    print  @dangling  ? "  DANGLING: @dangling"   : "";
    print  @nomatrix  ? "  MATRIX: @nomatrix"     : "";
    print  @untagged  ? sprintf("  untagged-normative: %d", scalar @untagged) : "";
    print  "\n";
    for my $u (@untagged) { print "    ? $u\n" }
}

printf "\n%d spec(s); %d untagged normative paragraph(s) to confirm by reading\n",
    $total_specs, $total_untagged;
